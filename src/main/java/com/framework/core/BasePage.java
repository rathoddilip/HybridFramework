package com.framework.core;

import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * BasePage — Parent class for all Page Objects.
 *
 * Provides:
 *  - Fluent waits with configurable timeouts
 *  - Safe click, type, select, hover, drag-drop actions
 *  - JavaScript fallbacks for stubborn elements
 *  - Screenshot capture for Allure
 *  - Scroll utilities
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final WebDriverWait shortWait;
    protected final Actions actions;
    private static final Logger log = LogManager.getLogger(BasePage.class);

    private static final int DEFAULT_TIMEOUT = 15;
    private static final int SHORT_TIMEOUT   = 5;

    protected BasePage() {
        try {
            this.driver    = DriverFactory.getDriver();
            this.wait      = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
            this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(SHORT_TIMEOUT));
            this.actions   = new Actions(driver);
            PageFactory.initElements(driver, this);
        } catch (IllegalStateException e) {
            log.error("WebDriver not initialized. Ensure @BeforeMethod calls DriverFactory.initDriver()");
            throw e;
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Step("Navigate to URL: {url}")
    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    // ── Waits ─────────────────────────────────────────────────────────────────

    public WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForVisible(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    public boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean waitForTextPresent(By locator, String text) {
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public void waitForPageLoad() {
        wait.until(driver -> (Boolean) ((JavascriptExecutor) driver)
                .executeScript("return document.readyState").equals("complete"));
    }

    // ── Click Actions ─────────────────────────────────────────────────────────

    @Step("Click element: {locator}")
    public void click(By locator) {
        log.debug("Clicking: {}", locator);
        waitForClickable(locator).click();
    }

    @Step("Click element")
    public void click(WebElement element) {
        waitForClickable(element).click();
    }

    /**
     * JS click fallback — useful for overlapping elements.
     */
    @Step("JS Click element: {locator}")
    public void jsClick(By locator) {
        WebElement element = waitForVisible(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    @Step("Double click element")
    public void doubleClick(WebElement element) {
        actions.doubleClick(waitForClickable(element)).perform();
    }

    @Step("Right click element")
    public void rightClick(WebElement element) {
        actions.contextClick(waitForClickable(element)).perform();
    }

    // ── Input Actions ─────────────────────────────────────────────────────────

    @Step("Type '{text}' into element: {locator}")
    public void type(By locator, String text) {
        log.debug("Typing '{}' into: {}", text, locator);
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    @Step("Type '{text}' into element")
    public void type(WebElement element, String text) {
        waitForVisible(element).clear();
        element.sendKeys(text);
    }

    @Step("Clear and type '{text}'")
    public void clearAndType(By locator, String text) {
        WebElement element = waitForVisible(locator);
        element.sendKeys(Keys.CONTROL + "a");
        element.sendKeys(Keys.DELETE);
        element.sendKeys(text);
    }

    @Step("Press key: {key}")
    public void pressKey(By locator, Keys key) {
        waitForVisible(locator).sendKeys(key);
    }

    // ── Dropdown ──────────────────────────────────────────────────────────────

    @Step("Select by visible text: {text}")
    public void selectByText(By locator, String text) {
        new Select(waitForVisible(locator)).selectByVisibleText(text);
    }

    @Step("Select by value: {value}")
    public void selectByValue(By locator, String value) {
        new Select(waitForVisible(locator)).selectByValue(value);
    }

    @Step("Select by index: {index}")
    public void selectByIndex(By locator, int index) {
        new Select(waitForVisible(locator)).selectByIndex(index);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public String getText(By locator) {
        return waitForVisible(locator).getText().trim();
    }

    public String getText(WebElement element) {
        return waitForVisible(element).getText().trim();
    }

    public String getAttribute(By locator, String attribute) {
        return waitForVisible(locator).getAttribute(attribute);
    }

    public List<WebElement> findAll(By locator) {
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
        return driver.findElements(locator);
    }

    public boolean isDisplayed(By locator) {
        try {
            return shortWait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    public boolean isEnabled(By locator) {
        try {
            return waitForVisible(locator).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    // ── Scroll ────────────────────────────────────────────────────────────────

    @Step("Scroll to element")
    public void scrollTo(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", element);
    }

    @Step("Scroll to element: {locator}")
    public void scrollTo(By locator) {
        scrollTo(driver.findElement(locator));
    }

    public void scrollToTop() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0,0);");
    }

    public void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    // ── Hover & Drag ──────────────────────────────────────────────────────────

    @Step("Hover over element")
    public void hover(WebElement element) {
        actions.moveToElement(waitForVisible(element)).perform();
    }

    @Step("Drag and drop")
    public void dragAndDrop(WebElement source, WebElement target) {
        actions.dragAndDrop(source, target).perform();
    }

    // ── Frame & Window ────────────────────────────────────────────────────────

    public void switchToFrame(By locator) {
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    public void switchToNewWindow() {
        String current = driver.getWindowHandle();
        driver.getWindowHandles().stream()
                .filter(h -> !h.equals(current))
                .findFirst()
                .ifPresent(h -> driver.switchTo().window(h));
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    public String getAlertText() {
        return wait.until(ExpectedConditions.alertIsPresent()).getText();
    }

    public void acceptAlert() {
        wait.until(ExpectedConditions.alertIsPresent()).accept();
    }

    public void dismissAlert() {
        wait.until(ExpectedConditions.alertIsPresent()).dismiss();
    }

    // ── JavaScript ────────────────────────────────────────────────────────────

    public Object executeJS(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    public void highlightElement(WebElement element) {
        executeJS("arguments[0].style.border='3px solid red'", element);
    }

    // ── Screenshot ────────────────────────────────────────────────────────────

    @Attachment(value = "Screenshot", type = "image/png")
    public byte[] takeScreenshot() {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    @Attachment(value = "Page Source", type = "text/html")
    public String capturePageSource() {
        return driver.getPageSource();
    }
}
