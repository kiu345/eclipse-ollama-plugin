
package com.github.kiu345.eclipse.eclipseai.commands;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Creatable;

import jakarta.inject.Inject;

@Creatable
public class ReadWebPageCommand {
    @Inject
    private ILog logger;

    public String readWebPage(String url) {
        String content = "";
        logger.warn("calling unimplemented function");
//        WebDriver driver = null;
//        try {
//            ChromeOptions options = new ChromeOptions();
//            options.addArguments("--headless"); // Run Chrome in headless mode
//            options.addArguments("--disable-gpu");
//            options.addArguments("--window-size=1920,1200");
//            options.addArguments("--ignore-certificate-errors");
//            options.addArguments("--silent");
//
//            driver = new ChromeDriver(options);
//            logger.info("Fetching web page: " + url);
//
//            driver.get(url);
//            // You may need to wait for the page to load or for JavaScript to
//            // execute
//
//            String pageSource = driver.getPageSource();
//
//            Document document = Jsoup.parse(pageSource);
//
//            for (Element body : document.getElementsByTag("body")) {
//                String bodyHTML = body.toString();
//                var converter = FlexmarkHtmlConverter.builder().build();
//                content += converter.convert(bodyHTML);
//            }
//            logger.info("Web page content " + url + "\n\n" + content);
//        }
//        catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        finally {
//            if (driver != null) {
//                driver.quit(); // Close the browser
//            }
//        }

        return content;
    }
}
