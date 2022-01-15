## **Running DEER**
To bundle DEER as a single jar file, do

mvn clean package shade:shade -Dmaven.test.skip=true
Then execute it using

java -jar deer-cli/target/deer-cli-${current-version}.jar path_to_config.ttl
## **Using Docker**
The Docker image declares two volumes:

/plugins - this is where plugins are dynamically loaded from
/data - this is where configuration as well as input/output data will reside
For running DEER server in Docker, we expose port 8080. The image accepts the same arguments as the deer-cli.jar, i.e. to run a configuration at ./my-configuration:

docker run -it --rm \
   -v $(pwd)/plugins:/plugins \
   -v $(pwd):/data dicegroup/deer:latest \
   /data/my-configuration.ttl
To run DEER server:

docker run -it --rm \
   -v $(pwd)/plugins:/plugins \
   -p 8080:8080 \
   -s
