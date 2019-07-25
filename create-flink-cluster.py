import os
from argparse import ArgumentParser


def main():
    """Create, scale and remove Flink containers. Script assumes default name of the network as flink_network"""
    parser = ArgumentParser()
    parser.add_argument("--version", help="To check the version of the script", action="store_true")
    parser.add_argument("--jobmanager", help="To create a jobmanager", action="store_true")
    parser.add_argument("--taskmanager", help="To create a taskmanager", action="store_true")
    parser.add_argument("--cpus", help="CPU limitation", type=float)
    parser.add_argument("--memory", help="Memory limitation", type=str)
    parser.add_argument("--scale", help="Scale task-managers", type=int)
    parser.add_argument("--remove", help="To remove all the containers", action="store_true")
    args = parser.parse_args()

    if args.version:
        print("flink-docker version 1.0")

    elif args.scale and args.cpus and args.memory:
        for i in range(args.scale):
            os.popen(
                "docker run -d --net=flink_network --env JOB_MANAGER_RPC_ADDRESS=jobmanager --cpus="
                + str(args.cpus) + " --memory=" + str(args.memory) + " --expose 6121 --expose 6122 flink taskmanager")

    elif args.cpus and args.memory:
        os.popen(
            "docker run -d --net=flink_network --env JOB_MANAGER_RPC_ADDRESS=jobmanager --cpus="
            + str(args.cpus) + " --memory=" + str(args.memory) + " --expose 6121 --expose 6122 flink taskmanager")

    elif args.jobmanager:
        os.popen("docker run -d --net=flink_network --name jobmanager --env JOB_MANAGER_RPC_ADDRESS=jobmanager "
                 "--expose 6123 -p 8081:8081 flink jobmanager")

    elif args.taskmanager:
        os.popen("docker run -d --net=flink_network --name taskmanager --env JOB_MANAGER_RPC_ADDRESS=jobmanager "
                 "--expose 6121 --expose 6122 flink taskmanager")

    elif args.cpus:
        os.popen("docker run -d --net=flink_network --env JOB_MANAGER_RPC_ADDRESS=jobmanager --cpus=" +
                 str(args.cpus) + " --expose 6121 --expose 6122 flink taskmanager")

    elif args.memory:
        os.popen("docker run -d --net=flink_network --env JOB_MANAGER_RPC_ADDRESS=jobmanager --memory=" +
                 str(args.memory) + " --expose 6121 --expose 6122 flink taskmanager")

    elif args.scale:
        for i in range(args.scale):
            os.popen(
                "docker run -d --net=flink_network --env JOB_MANAGER_RPC_ADDRESS=jobmanager --expose 6121 "
                "--expose 6122 flink taskmanager")

    elif args.remove:
        os.popen("docker rm $(docker ps -aq) -f")


if __name__ == '__main__':
    main()

