import groovy.json.JsonOutput
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

import javax.servlet.MultipartConfigElement
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

import static spark.Spark.*

class Bootstrap {

    static ConfigObject loadConfig(String environment) {
        def configFile = new File("src/main/groovy/conf/config-${environment}.groovy")
        if( !configFile.exists() ) {
            throw new Exception("Config file missing.\n\t-> You have attempted to load a config file from '${configFile.canonicalPath}' but none was found.\n\t-> Please see '${configFile.canonicalPath.replace('config-'+environment+'.groovy', 'config-template.groovy')}' in that directory,\n\t-> make a copy, rename it for this environment and populate it as necessary.")
        }
        return new ConfigSlurper(environment).parse(configFile.toURI().toURL())
    }

    static void main(String[] args) {

        def environment = System.getProperty('environment') ?: 'dev'
        def config = loadConfig(environment)

        staticFileLocation('/public')
        staticFiles.expireTime(10)
        port(9000)

        before "*", { req, res ->
            res.header("Access-Control-Allow-Headers", "Authorization, Content-Type")
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            res.header("Access-Control-Allow-Origin", "*")
            res.type("application/json")
        }

        options "*/*", { req, res ->
            String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        }

        post "/tweet", { req, res ->
            def uploadDir = new File(System.getProperty("java.io.tmpdir"))
            Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "")
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"))
            def upload = req.raw().getPart("uploadFile")

            InputStream is = upload.getInputStream()
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING)
            def msg = req.queryMap().get('tweet').value();

            ConfigurationBuilder twitterConfigBuilder = new ConfigurationBuilder();
            twitterConfigBuilder.setDebugEnabled(true)
            twitterConfigBuilder.setOAuthConsumerKey(config.codes.recursive.twitter.OAuthConsumerKey)
            twitterConfigBuilder.setOAuthConsumerSecret(config.codes.recursive.twitter.OAuthConsumerSecret)
            twitterConfigBuilder.setOAuthAccessToken(config.codes.recursive.twitter.OAuthAccessToken)
            twitterConfigBuilder.setOAuthAccessTokenSecret(config.codes.recursive.twitter.OAuthAccessTokenSecret)

            Twitter twitter = new TwitterFactory(twitterConfigBuilder.build()).getInstance()

            StatusUpdate status = new StatusUpdate(msg)
            status.setMedia(tempFile.toFile())
            twitter.updateStatus(status)
            tempFile.toFile().delete()

            return JsonOutput.toJson([success: true])
        }

    }

}