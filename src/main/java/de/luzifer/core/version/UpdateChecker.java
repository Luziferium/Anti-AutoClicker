package de.luzifer.core.version;

import de.luzifer.core.utils.InputStreamUtils;
import lombok.AllArgsConstructor;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

@AllArgsConstructor
public class UpdateChecker {
    
    private static final String URL_LINK = "https://raw.githubusercontent.com/Luziferium/Anti-Auto-Clicker/master/src/main/resources/version.txt";
    
    private final Logger logger;
    private final String currentVersion;
    
    public UpdateCheckerResult checkUpdate() {

        String latestVersion;
        
        try {

            HttpURLConnection connection = (HttpURLConnection) new URL(URL_LINK).openConnection();
            connection.connect();
            
            latestVersion = InputStreamUtils.readLineFromInputStream(connection.getInputStream(), logger);
            
        } catch (Exception e) {
            
            logger.warning("This seems wrong, either GitHub is down, the repository private or something else happened");
            return new UpdateCheckerResult(null, null, false);
        }
        
        return new UpdateCheckerResult(latestVersion, currentVersion, !latestVersion.equals(currentVersion));
    }
    
}
