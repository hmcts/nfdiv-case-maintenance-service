provider "azurerm" {
    features {}
}

locals {
  vault_name = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "div_key_vault" {
  name                = local.vault_name
  resource_group_name = local.vault_name
}

data "azurerm_key_vault_secret" "idam-secret" {
  name      = "idam-secret"
  key_vault_id = data.azurerm_key_vault.div_key_vault.id
}

data "azurerm_key_vault_secret" "idam-caseworker-username" {
  name      = "idam-caseworker-username"
  key_vault_id = data.azurerm_key_vault.div_key_vault.id
}

data "azurerm_key_vault_secret" "idam-caseworker-password" {
  name      = "idam-caseworker-password"
  key_vault_id = data.azurerm_key_vault.div_key_vault.id
}

# Copy S2S key from S2S vault to shared vault
data "azurerm_key_vault" "s2s_vault" {
    name = "s2s-${var.env}"
    resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault_secret" "s2s_key" {
    name      = "microservicekey-nfdiv-cms"
    key_vault_id = "${data.azurerm_key_vault.s2s_vault.id}"
}

resource "azurerm_key_vault_secret" "local_s2s_key" {
    name         = "cms-service-key"
    value        = "${data.azurerm_key_vault_secret.s2s_key.value}"
    key_vault_id = "${data.azurerm_key_vault.div_key_vault.id}"
}
