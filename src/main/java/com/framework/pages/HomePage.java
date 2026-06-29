package com.framework.pages;

import com.framework.core.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * HomePage — Fixerra Investment App
 *
 * URL: app base URL + /home/active
 *
 * Sections visible from screenshot:
 *  - Portfolio balance (₹0 + ₹0 | View FDs)
 *  - Discover Fixed Deposits (Women Benefits, Best Rates, Popular FDs, Senior Citizen)
 *  - FD Plans with tabs (Max Returns | 0-1 year | 1-3 years | >3 years)
 *  - FD listings (bank name, interest rate, min amount, badges)
 *  - Senior Citizen toggle
 */
public class HomePage extends BasePage {

    // ── Header ────────────────────────────────────────────────────────────────

    @FindBy(css = ".fixerra-logo, [class*='logo']")
    private WebElement fixerraLogo;

    @FindBy(css = "[class*='menu'], [class*='hamburger'], button[aria-label='menu']")
    private WebElement menuButton;

    // ── Portfolio Balance ─────────────────────────────────────────────────────

    @FindBy(css = "[class*='balance'], [class*='portfolio-value']")
    private WebElement portfolioBalanceWidget;

    @FindBy(css = "[class*='view-fd'], a[href*='fd']")
    private WebElement viewFdsLink;

    // ── Discover FD Categories ────────────────────────────────────────────────

    private final By discoverFdSection   = By.cssSelector("[class*='discover'], [class*='category']");
    private final By womenBenefitsCard   = By.cssSelector("[class*='women'], img[alt*='women']");
    private final By bestRatesCard       = By.cssSelector("[class*='best-rate'], img[alt*='best']");
    private final By popularFdsCard      = By.cssSelector("[class*='popular'], img[alt*='popular']");
    private final By seniorCitizenCard   = By.cssSelector("[class*='senior'], img[alt*='senior']");

    // ── FD Plan Tabs ──────────────────────────────────────────────────────────

    private final By maxReturnsTab       = By.cssSelector("[class*='max-return'], button.tab");
    private final By zeroToOneYearTab    = By.xpath("//button[contains(text(),'0-1 year')]");
    private final By oneToThreeYearTab   = By.xpath("//button[contains(text(),'1-3 year')]");
    private final By aboveThreeYearTab   = By.xpath("//button[contains(text(),'> 3 year')]");
    private final By seniorCitizenToggle = By.cssSelector("[class*='senior-toggle'] input, input[type='checkbox']");

    // ── FD Listings ───────────────────────────────────────────────────────────

    private final By fdListItems         = By.cssSelector("[class*='fd-card'], [class*='fd-item'], [class*='bank-card']");
    private final By bankNames           = By.cssSelector("[class*='bank-name'], [class*='issuer-name']");
    private final By interestRates       = By.cssSelector("[class*='interest-rate'], [class*='rate']");
    private final By minAmounts          = By.cssSelector("[class*='min-amount'], [class*='minimum']");
    private final By highestRateBadge    = By.cssSelector("[class*='highest-rate'], [class*='badge']");
    private final By instantBookingBadge = By.cssSelector("[class*='instant'], [class*='instant-booking']");

    // ── Page State ────────────────────────────────────────────────────────────

    private final By homeContainer      = By.xpath("//h1[contains(.,'Welcome to') and contains(.,'Fixerra')]");
    private final By loadingSpinner     = By.cssSelector("[class*='loader'], [class*='spinner']");

    // ── Page Actions ──────────────────────────────────────────────────────────

    @Step("Wait for home page to fully load")
    public HomePage waitForHomePageToLoad() {
        waitForInvisible(loadingSpinner);
        waitForVisible(homeContainer);
        waitForPageLoad();
        return this;
    }

    // ── Portfolio ─────────────────────────────────────────────────────────────

    @Step("Get portfolio balance text")
    public String getPortfolioBalance() {
        return getText(portfolioBalanceWidget);
    }

    @Step("Click View FDs")
    public void clickViewFDs() {
        click(viewFdsLink);
        waitForPageLoad();
    }

    // ── FD Category Navigation ────────────────────────────────────────────────

    @Step("Click Women Benefits category")
    public void clickWomenBenefits() {
        click(womenBenefitsCard);
    }

    @Step("Click Best Rates category")
    public void clickBestRates() {
        click(bestRatesCard);
    }

    @Step("Click Popular FDs category")
    public void clickPopularFDs() {
        click(popularFdsCard);
    }

    @Step("Click Senior Citizen category")
    public void clickSeniorCitizen() {
        click(seniorCitizenCard);
    }

    // ── FD Plan Tabs ──────────────────────────────────────────────────────────

    @Step("Click Max Returns tab")
    public HomePage clickMaxReturnsTab() {
        click(maxReturnsTab);
        return this;
    }

    @Step("Click 0-1 Year tab")
    public HomePage clickZeroToOneYearTab() {
        click(zeroToOneYearTab);
        return this;
    }

    @Step("Click 1-3 Years tab")
    public HomePage clickOneToThreeYearTab() {
        click(oneToThreeYearTab);
        return this;
    }

    @Step("Click > 3 Years tab")
    public HomePage clickAboveThreeYearTab() {
        click(aboveThreeYearTab);
        return this;
    }

    @Step("Toggle Senior Citizen filter")
    public HomePage toggleSeniorCitizen() {
        click(seniorCitizenToggle);
        return this;
    }

    // ── FD Listing Data ───────────────────────────────────────────────────────

    @Step("Get total FD listings count")
    public int getFdListingsCount() {
        return findAll(fdListItems).size();
    }

    @Step("Get all bank names from FD list")
    public List<String> getAllBankNames() {
        return findAll(bankNames)
                .stream()
                .map(WebElement::getText)
                .filter(text -> !text.isBlank())
                .collect(Collectors.toList());
    }

    @Step("Get all interest rates from FD list")
    public List<String> getAllInterestRates() {
        return findAll(interestRates)
                .stream()
                .map(WebElement::getText)
                .filter(text -> !text.isBlank())
                .collect(Collectors.toList());
    }

    @Step("Get interest rate at index: {index}")
    public String getInterestRateAt(int index) {
        List<WebElement> rates = findAll(interestRates);
        return rates.get(index).getText();
    }

    @Step("Get minimum amount at index: {index}")
    public String getMinAmountAt(int index) {
        List<WebElement> amounts = findAll(minAmounts);
        return amounts.get(index).getText();
    }

    @Step("Click FD listing at index: {index}")
    public void clickFdListingAt(int index) {
        List<WebElement> listings = findAll(fdListItems);
        scrollTo(listings.get(index));
        click(listings.get(index));
        waitForPageLoad();
    }

    // ── Validations ───────────────────────────────────────────────────────────

    public boolean isHomePageDisplayed() {
        return isDisplayed(homeContainer);
    }

    public boolean isDiscoverFdSectionDisplayed() {
        return isDisplayed(discoverFdSection);
    }

    public boolean isFdListingDisplayed() {
        return getFdListingsCount() > 0;
    }

    public boolean isHighestRateBadgeVisible() {
        return isDisplayed(highestRateBadge);
    }

    public boolean isInstantBookingBadgeVisible() {
        return isDisplayed(instantBookingBadge);
    }


//    public String getCurrentUrl() {
//        return getDriver().getCurrentUrl();
//    }
}
