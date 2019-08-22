package loci.containerize.container

import java.io.File
import java.nio.file.{Path, Paths}

import loci.containerize.{Check, Options}
import loci.containerize.IO._
import loci.containerize.main.Containerize
import loci.containerize.types.TempLocation
import sys.process._

class Compose[+C <: Containerize](io : IO)(buildDir : File)(implicit plugin : C) {

  def getComposer(multiTierModuleName : String, dirs : List[TempLocation]) : compose = new compose(multiTierModuleName, dirs)

  class compose(multiTierModuleName : String, dirs : List[TempLocation]){
    val logger : Logger = plugin.logger
    var composePath : File = _

    io.createDir(Paths.get(buildDir.getAbsolutePath, Options.composeDir)) match{
      case Some(f) => composePath = f
      case None => logger.error(s"Could not create composer build directory at: ${ buildDir.getAbsolutePath + "/" + Options.composeDir }")
    }

    //todo interestings:
    // - see constraints and prefs for placement (e.g. user defined sec level
    // - extra_hosts
    // - health_check, also in DOCKERRFILE
    // - logging
    // - ip, aliases for versions or something?
    // - ports long syntax
    // - secrets (...?)
    // - VOLUMES!
    def buildDockerCompose() : Unit = {
      val CMD =
        "version: \"3.7\"\n" +
          dirs.foldLeft("services:\n"){ (s, d) =>
            val cfg : ContainerConfig[C] = new ContainerConfig[C](d.entryPoint.config)(io, plugin)
            s +
             s"""  ${ d.getImageName }:
              |    # configuration for ${ d.getImageName } (${ cfg.getConfigType }) 
              |    image: ${ Options.dockerUsername }/${ Options.dockerRepository.toLowerCase }:${ d.getImageName }
              |    deploy:
              |      mode: ${ cfg.getDeployMode }
              |      replicas: ${ cfg.getReplicas }
              |      resources:
              |        limits:
              |          cpus: "${ cfg.getCPULimit }"
              |          memory: ${ cfg.getMemLimit }
              |        reservations:
              |          cpus: "${ cfg.getCPUReserve }"
              |          memory: ${ cfg.getMemReserve }
              |      restart_policy:
              |        condition: any
              |      rollback_config:
              |        order: start-first
              |      update_config:
              |        parallelism: 2
              |        failure_action: rollback
              |        order: start-first
              |    labels:
              |      ${ Options.labelPrefix }.module: "$multiTierModuleName"
              |""" +
              (if(d.entryPoint.endPoints.exists(_.way != "connect"))
             s"    ${ d.entryPoint.endPoints.foldLeft("ports:\n")((s, e) => if(e.way == "connect" && Check ? e.port) s else s + "      - \"" + e.port + ":" + e.port + "\"\n") }"
              else "") +
              s"""    networks:
              |      ${Options.swarmName}:
              |        aliases:
              |          - ${ d.getImageName }
              |      ${multiTierModuleName}:
              |        aliases:
              |          - ${ d.getImageName }
              |"""
          } +
          s"""networks:
          |  ${Options.swarmName}:
          |    external: true
          |  ${multiTierModuleName}:
          |    driver: overlay
          |    attachable: true
          |    internal: false
          |    name: ${multiTierModuleName}
          |"""

      /**
       * monitor_service:
       * # configuration for monitoring service, running on each master node.
       * image: alexellis2/visualizer-arm:latest
       * deploy:
       * mode: global
       * placement:
       * constraints: [node.role == manager]
       * ports:
       *       - "8080:8080"
       * volumes:
       *       - type: bind
       * source: /var/run/docker.sock
       * target: /var/run/docker.sock
       */
              /**
              |    healthcheck: //todo in dockerfile?
              |      test: [\"CMD\", \"curl\", \"-f\", \"127.0.0.1\"]
              |      interval: 2m
              |      timeout: 15s
              |      retries: 3
              |      start_period: 1m
               ***/
      /**
          "sysctl:\n" +
          " net.ipv4.conf.eth0.route_localnet:1\n" +
          "cap_add:\n" +
          " - NET_ADMIN\n" +
          " - NET_RAW\n"
        */

      io.buildFile(CMD.stripMargin, Paths.get(composePath.getAbsolutePath, multiTierModuleName + ".yml"))
    }
    def buildDockerSwarm(multiTierModules : List[String]) : Unit = {
      val CMD =
        s"""docker node ls > /dev/null 2>&1 | grep "Leader"
           |if [ $$? -ne 0 ]; then
           |  docker swarm init
           |fi
           |docker network inspect ${Options.swarmName} > /dev/null 2>&1
           |if [ $$? -eq 0 ]; then
           |  docker network rm ${Options.swarmName} > /dev/null 2>&1
           |  if [ $$? -ne 0 ]; then
           |    echo "Could not remove network ${Options.swarmName}. Continuing with the old network. Remove network manually to update it next time."
           |  fi
           |fi
           |docker network create -d overlay --attachable=true ${Options.swarmName}
           |echo "---------------------------------------------"
           |echo ">>> Creating stacks from compose files... <<<"
           |echo "---------------------------------------------"
           |""" +
            multiTierModules.foldLeft("")((M, m) => M + {
              s"docker stack deploy -c $m.yml $m \n" +
                "if [ $? -eq 0 ]; then\n" +
                "  echo \"Successfully deployed stack '" + m + ".'\"\n" +
                "  else\n" +
                "    echo \"Error while deploying stack '" + m + "', aborting now. Please fix before retrying.\"\n" +
                "    exit 1\n" + 
                "fi\n"
            }) +   //${Options.swarmName}
        s"""docker service create --publish 8080:8080 --mode global --constraint 'node.role == manager' --mount type=bind,source=/var/run/docker.sock,destination=/var/run/docker.sock --name monitor_service alexellis2/visualizer-arm:latest
           |  docker service inspect ${Options.swarmName}_monitor_service > /dev/null 2>&1
           |if [ $$? -eq 0 ]; then
           |  echo "----------------------------------------------------------------------------------"
           |  echo ">>> Swarm Visualizer running on each master node, reachable at: localhost:8080 <<<"
           |  echo "----------------------------------------------------------------------------------"
           |fi
           |echo "-----------------------"
           |echo ">>> Nodes in Swarm: <<<"
           |echo "-----------------------"
           |docker node ls
           |docker swarm join-token manager
           |docker swarm join-token worker
           |echo "--------------------------"
           |echo ">>> Services in Swarm: <<<"
           |echo "--------------------------"
           |docker service ls
           |echo "--------------------------"
           |echo ">> PRESS ANY KEY TO CONTINUE / CLOSE <<"
           |read -n 1 -s
           |exit 0
           |else
           |  exit 1
           |"""

      io.buildFile(io.buildScript(CMD.stripMargin), Paths.get(composePath.getAbsolutePath, "swarm-init.sh"))
    }

    def runDockerSwarm() : Unit = {
      Process("cmd /k start bash swarm-init.sh", composePath).!!(logger.strong) //todo really make this blocking?
    }
    //$ docker service rm my-nginx
    //$ docker network rm nginx-net nginx-net-2
  }
}
