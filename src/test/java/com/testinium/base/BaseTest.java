package com.testinium.base;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.testinium.driver.TestiniumSeleniumDriver;
import com.testinium.model.ElementInfo;
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
        try {
            if (StringUtils.isEmpty(System.getenv("key"))) {
                logger.info("Local cihazda " + selectPlatform + " ortamında " + browserName + " browserında test ayağa kalkacak");
                if ("win".equalsIgnoreCase(selectPlatform)) {
                    if ("chrome".equalsIgnoreCase(browserName)) {
                        driver = new ChromeDriver(chromeOptions());
                        driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
                    } else if ("firefox".equalsIgnoreCase(browserName)) {
                        driver = new FirefoxDriver(firefoxOptions());
                        driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
                    }
                } else if ("mac".equalsIgnoreCase(selectPlatform)) {
                    if ("chrome".equalsIgnoreCase(browserName)) {
                        driver = new ChromeDriver(chromeOptions());
                        driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
                    } else if ("firefox".equalsIgnoreCase(browserName)) {
                        driver = new FirefoxDriver(firefoxOptions());
                        driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
                    }
                    actions = new Actions(driver);
                }

            } else {
                logger.info("************************************   Testiniumda test ayağa kalkacak   ************************************");
                ChromeOptions options = new ChromeOptions();
                capabilities = DesiredCapabilities.chrome();
                options.setExperimentalOption("w3c", false);
                options.addArguments("disable-translate");
                options.addArguments("--disable-notifications");
                options.addArguments("--start-fullscreen");
                Map<String, Object> prefs = new HashMap<>();
                options.setExperimentalOption("prefs", prefs);
                capabilities.setCapability(ChromeOptions.CAPABILITY, options);
                capabilities.setCapability("key", System.getenv("key"));
                browserName = System.getenv("browser");
                driver = new TestiniumSeleniumDriver(new URL("http://172.25.1.110:4444/wd/hub"), capabilities);
                Thread.sleep(5000);
                actions = new Actions(driver);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
        chromeOptions = new ChromeOptions();
        capabilities = DesiredCapabilities.chrome();
        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.addArguments("--kiosk");
        chromeOptions.addArguments("--disable-notifications");
        chromeOptions.addArguments("--start-fullscreen");
        System.setProperty("webdriver.chrome.driver", "web_driver/chromedriver.exe");
        chromeOptions.merge(capabilities);
        return chromeOptions;
    }

    /**
     * Set Firefox options
     *
     * @return the firefox options
     */
    public FirefoxOptions firefoxOptions() {
        firefoxOptions = new FirefoxOptions();
        capabilities = DesiredCapabilities.firefox();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        firefoxOptions.addArguments("--kiosk");
        firefoxOptions.addArguments("--disable-notifications");
        firefoxOptions.addArguments("--start-fullscreen");
        FirefoxProfile profile = new FirefoxProfile();
        capabilities.setCapability(FirefoxDriver.PROFILE, profile);
        capabilities.setCapability("marionette", true);
        firefoxOptions.merge(capabilities);
        System.setProperty("webdriver.gecko.driver", "web_driver/geckodriver");
        return firefoxOptions;
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
