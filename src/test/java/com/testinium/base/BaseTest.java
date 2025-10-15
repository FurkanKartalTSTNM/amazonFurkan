package com.testinium.base;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.testinium.driver.TestiniumSeleniumDriver;
import com.testinium.model.ElementInfo;
import com.testinium.util.TestiniumEnvironment;
import com.thoughtworks.gauge.AfterScenario;
import com.thoughtworks.gauge.BeforeScenario;
import org.openqa.selenium.WebDriver;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


public class BaseTest {

    protected static WebDriver driver;
    protected static Actions actions;
    protected Logger logger = LoggerFactory.getLogger(getClass());
    DesiredCapabilities capabilities;
    ChromeOptions chromeOptions;
    FirefoxOptions firefoxOptions;

    String browserName = "chrome";
    String selectPlatform = "mac";

    private static final String DEFAULT_DIRECTORY_PATH = "elementValues";
    ConcurrentMap<String, Object> elementMapList = new ConcurrentHashMap<>();

    @BeforeScenario
    public void setUp() {
        logger.info("************************************  BeforeScenario  ************************************");
        TestiniumEnvironment.init();
        try {
            boolean isRemote = StringUtils.isNotBlank(System.getenv("key"));

            if (!isRemote) {
                logger.info("Local cihazda {} ortamında {} browserında test ayağa kalkacak",
                        selectPlatform, browserName);

                if ("chrome".equalsIgnoreCase(browserName)) {
                    driver = new ChromeDriver(chromeOptions());
                } else if ("firefox".equalsIgnoreCase(browserName)) {
                    driver = new FirefoxDriver(firefoxOptions());
                } else {
                    throw new IllegalArgumentException("Unsupported browser: " + browserName);
                }

            } else {
                logger.info("************************************   Testiniumda test ayağa kalkacak   ************************************");
                ChromeOptions options = chromeOptions();
                // Testinium anahtarı gerekiyorsa Options üstünden ver:
                String key = System.getenv("key");
                if (StringUtils.isBlank(key)) {
                    logger.warn("Environment variable 'key' is empty, proceeding without it");
                } else {
                    options.setCapability("testinium:key", key);
                }

                // İstersen browser'ı env'den oku
                browserName = System.getenv("browser");

                // RemoteWebDriver yerine kendi TestiniumSeleniumDriver'ını options ile başlat
                driver = new TestiniumSeleniumDriver(new URL("http://172.25.1.110:4444/wd/hub"), options);
            }

            // Selenium 4 timeout API
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            actions = new Actions(driver);

        }
    }

    @AfterScenario
    public void tearDown() {
        driver.quit();
    }

    public void initMap(File[] fileList) {
        Type elementType = new TypeToken<List<ElementInfo>>() {
        }.getType();
        Gson gson = new Gson();
        List<ElementInfo> elementInfoList = null;
        for (File file : fileList) {
            try {
                elementInfoList = gson
                        .fromJson(new FileReader(file), elementType);
                elementInfoList.parallelStream()
                        .forEach(elementInfo -> elementMapList.put(elementInfo.getKey(), elementInfo));
            } catch (FileNotFoundException e) {
                logger.warn("{} not found", e);
            }
        }
    }

    public File[] getFileList() {
        File[] fileList = new File(
                this.getClass().getClassLoader().getResource(DEFAULT_DIRECTORY_PATH).getFile())
                .listFiles(pathname -> !pathname.isDirectory() && pathname.getName().endsWith(".json"));
        if (fileList == null) {
            logger.warn(
                    "File Directory Is Not Found! Please Check Directory Location. Default Directory Path = {}",
                    DEFAULT_DIRECTORY_PATH);
            throw new NullPointerException();
        }
        return fileList;
    }

    /**
     * Set Chrome options
     *
     * @return the chrome options
     */
    public ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        options.setExperimentalOption("prefs", prefs);

        // W3C kapatma YOK; kaldırıldı
        options.addArguments("--disable-notifications", "--start-fullscreen", "--kiosk");

        // Testinium veya Grid'e özel capability gerekiyorsa Options üstünden ver:
        // options.setCapability("testinium:key", System.getenv("key"));

        // Selenium 4'te DesiredCapabilities.merge() kullanma; Options zaten Capability taşıyor.
        return options;
    }

    /**
     * Set Firefox options
     *
     * @return the firefox options
     */
    public FirefoxOptions firefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();

        FirefoxProfile profile = new FirefoxProfile();
        // profil ayarların varsa burada yap
        options.setProfile(profile);                // <- ARTIK BÖYLE

        options.addArguments("--kiosk", "--start-fullscreen", "--disable-notifications");

        // Marionette flag'ini set etmene gerek yok; varsayılan marionette.
        // options.setCapability("marionette", true); // gereksiz

        return options;
    }

    public ElementInfo findElementInfoByKey(String key) {
        return (ElementInfo) elementMapList.get(key);
    }

    public void saveValue(String key, String value) {
        elementMapList.put(key, value);
    }

    public String getValue(String key) {
        return elementMapList.get(key).toString();
    }

}
