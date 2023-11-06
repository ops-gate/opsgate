import { test, expect } from "@playwright/test";

test.beforeEach("visit page and login", async ({ page }) => {
  await page.goto("/");
  await page.getByLabel("Email").click();
  await page.getByLabel("Email").fill("testUser@example.com");
  await page.getByLabel("Email").press("Tab");
  await page.getByLabel("Password").fill("testPassword");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await page.waitForURL("**/requests");
});

test("Create Connection", async ({ page }) => {
  await page.getByRole("link", { name: "Settings" }).click({ force: true });
  await page.waitForURL("**/settings");
  await page.getByRole("button", { name: "Add Datasource" }).click();
  await page.getByPlaceholder("Database Name").click();
  await page.getByPlaceholder("Database Name").fill("My Test Datasource");
  await page.getByLabel("Database Engine").selectOption("POSTGRESQL");
  await page.getByPlaceholder("localhost").fill("postgres");
  await page.getByPlaceholder("5432").fill("5432");
  await page.getByRole("button", { name: "Add", exact: true }).click();
  await page.getByText("My Test Datasourcepostgres").click();
  await page.getByRole("button", { name: "Add Connection" }).click();
  await page.getByPlaceholder("Connection Name").click();
  await page.getByPlaceholder("Connection Name").fill("my test connection");
  await page.getByPlaceholder("Connection Name").press("Tab");
  await page.getByPlaceholder("readonly").fill("postgres");
  await page.getByPlaceholder("readonly").press("Tab");
  await page.getByPlaceholder("password").fill("postgres");
  await page.getByRole("button", { name: "Add", exact: true }).click();
  await page.getByRole("spinbutton").click();
  await page.getByRole("spinbutton").fill("0");
  await page.getByRole("button").nth(4).click();
  await expect(page.getByText("My Test Datasource")).toBeVisible();
  await expect(page.getByText("my test connection")).toBeVisible();
  await expect(page.getByRole("spinbutton")).toHaveValue("0");
});

test("Create Request", async ({ page }) => {
  await page.getByRole("link", { name: "New", exact: true }).click();
  await page.getByRole("button", { name: "Query" }).nth(1).click();
  await page.getByPlaceholder("My query").click();
  await page.getByPlaceholder("My query").fill("my test query");
  await page
    .getByPlaceholder("What are you trying to accomplish with this Query?")
    .click();
  await page
    .getByPlaceholder("What are you trying to accomplish with this Query?")
    .fill("Testing if everything works");
  await page
    .getByPlaceholder("What are you trying to accomplish with this Query?")
    .press("Tab");
  await page
    .getByPlaceholder("Select id from some_table;")
    .fill("Select * from test;");
  await page.getByRole("button", { name: "Submit" }).click();
  await page.waitForURL("**/requests");
  await expect(
    page.getByRole("link").filter({ hasText: "Testing if everything works" })
  ).toBeVisible();
});

test("Execute Request", async ({ page }) => {
  await page
    .getByRole("link")
    .filter({ hasText: "Test Query" })
    .first()
    .click();
  await page.getByRole("button", { name: "Run Query" }).click();
  await page.getByRole("cell").nth(1).waitFor();
  await expect(await page.getByRole("cell").count()).toBeGreaterThan(4);
  await expect(page.getByRole("cell", { name: "id" })).toBeVisible();
});
