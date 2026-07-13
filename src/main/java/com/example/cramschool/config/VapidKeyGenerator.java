package com.example.cramschool.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import nl.martijndwars.webpush.Utils;

public final class VapidKeyGenerator {

	private VapidKeyGenerator() {
	}

	public static VapidKeyPair generate() {
		try {
			if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
				Security.addProvider(new BouncyCastleProvider());
			}
			ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec(Utils.CURVE);
			KeyPairGenerator generator = KeyPairGenerator.getInstance(Utils.ALGORITHM,
					BouncyCastleProvider.PROVIDER_NAME);
			generator.initialize(parameterSpec);
			KeyPair keyPair = generator.generateKeyPair();
			return new VapidKeyPair(
					Base64.getUrlEncoder().encodeToString(Utils.encode((ECPublicKey) keyPair.getPublic())),
					Base64.getUrlEncoder().encodeToString(Utils.encode((ECPrivateKey) keyPair.getPrivate())));
		} catch (Exception ex) {
			throw new IllegalStateException("無法產生 Web Push VAPID 金鑰", ex);
		}
	}

	public record VapidKeyPair(String publicKey, String privateKey) {
	}
}
