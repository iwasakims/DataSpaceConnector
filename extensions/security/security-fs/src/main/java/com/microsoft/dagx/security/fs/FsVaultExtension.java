package com.microsoft.dagx.security.fs;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.VaultExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static com.microsoft.dagx.security.fs.FsConfiguration.KEYSTORE_LOCATION;
import static com.microsoft.dagx.security.fs.FsConfiguration.KEYSTORE_PASSWORD;
import static com.microsoft.dagx.security.fs.FsConfiguration.VAULT_LOCATION;

/**
 * Bootstraps the file system-based vault extension.
 */
public class FsVaultExtension implements VaultExtension {
    private Vault vault;
    private PrivateKeyResolver privateKeyResolver;

    @Override
    public void initialize(Monitor monitor) {
        vault = initializeVault();
        privateKeyResolver = initializeResolver();
        monitor.info("Initialized FS Vault extension");
    }

    @Override
    public Vault getVault() {
        return vault;
    }

    @Override
    public PrivateKeyResolver getPrivateKeyResolver() {
        return privateKeyResolver;
    }

    private Vault initializeVault() {
        var vaultPath = Paths.get(VAULT_LOCATION);
        if (!Files.exists(vaultPath)) {
            throw new DagxException("Vault file does not exist: " + VAULT_LOCATION);
        }
        return new FsVault(vaultPath);
    }

    private PrivateKeyResolver initializeResolver() {
        var keyStorePath = Paths.get(KEYSTORE_LOCATION);
        if (!Files.exists(keyStorePath)) {
            throw new DagxException("Key store does not exist: " + KEYSTORE_LOCATION);
        }

        try (InputStream stream = Files.newInputStream(keyStorePath)) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(stream, KEYSTORE_PASSWORD.toCharArray());
            return new FsPrivateKeyResolver(KEYSTORE_PASSWORD, keyStore);
        } catch (IOException | GeneralSecurityException e) {
            throw new DagxException(e);
        }
    }

}
