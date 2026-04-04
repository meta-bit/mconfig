Build:

    mvn -U -DskipTests package

Run:

    java -jar target/mconfig-spring-demo-0.0.1-SNAPSHOT.jar
    Or via maven: mvn spring-boot:run

Try it:

    Open http://localhost:8081/conn — it reads live values from mConfig.
    Edit the resource file or (for a real runtime test) mount a file in the discovered config path (or use test-mode paths) and change server.port/server.host — mConfig will pick up changes and adapter returns updated values.
    or use mconfig CLI tool to change values:
