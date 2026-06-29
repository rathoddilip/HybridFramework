package com.tests.web;

import com.framework.config.ConfigManager;
import com.framework.core.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Smoke Tests")
public class SmokeTest extends BaseTest {

    @Test(groups = {"smoke", "web"})
    @Description("Verify application URL loads successfully")
    public void verifyAppLoads() {
        String appUrl = ConfigManager.get("app.baseUrl");
        getDriver().get(appUrl);

        String title = getDriver().getTitle();
        System.out.println("Page Title: " + title);

        assertThat(title).isNotEmpty();
    }
}