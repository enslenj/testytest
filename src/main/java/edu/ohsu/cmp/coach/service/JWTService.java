package edu.ohsu.cmp.coach.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.MyHttpException;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.fhir.jwt.AccessToken;
import edu.ohsu.cmp.coach.util.CryptoUtil;
import edu.ohsu.cmp.coach.util.UUIDUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JWTService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${fhir.security.jwt.client-id}")
    private String clientId;

    @Value("${fhir.security.jwt.x509-certificate-file:}")
    private String x509CertificateFilename;

    @Value("${fhir.security.jwt.pkcs8-private-key-file:}")
    private String pkcs8PrivateKeyFilename;

    public boolean isJWTEnabled() {
        return StringUtils.isNotBlank(clientId) &&
                StringUtils.isNotBlank(x509CertificateFilename) &&
                StringUtils.isNotBlank(pkcs8PrivateKeyFilename);
    }

    public String createToken(String tokenAuthUrl) throws ConfigurationException {
        if ( ! isJWTEnabled() ) return null;

        // iss: clientId
        // sub: clientId (same as iss)
        // aud: tokenAuthUrl
        // jti: uuid, max 151 chars
        // exp: 5 minutes in the future, expressed as an integer 5 minutes in the future

        File x509CertificateFile = new File(x509CertificateFilename);
        File pkcs8PrivateKeyFile = new File(pkcs8PrivateKeyFilename);

        try {
            RSAPublicKey publicKey = (RSAPublicKey) CryptoUtil.readPublicKeyFromCertificate(x509CertificateFile);
            RSAPrivateKey privateKey = (RSAPrivateKey) CryptoUtil.readPrivateKey(pkcs8PrivateKeyFile);
            Algorithm algorithm = Algorithm.RSA384(publicKey, privateKey);

            return JWT.create()
                    .withIssuer(clientId)
                    .withSubject(clientId)
                    .withAudience(tokenAuthUrl)
                    .withJWTId(UUIDUtil.getRandomUUID())
                    .withExpiresAt(buildExpiresAt())
                    .sign(algorithm);

        } catch (Exception e) {
            throw new ConfigurationException("could not instantiate object with iss=" + tokenAuthUrl +
                    ", x509CertificateFile=" + x509CertificateFile +
                    ", pkcs8PrivateKeyFile=" + pkcs8PrivateKeyFile, e);
        }
    }

    public boolean isTokenValid(String token, String iss) {
        if ( ! isJWTEnabled() ) return false;

        File x509CertificateFile = new File(x509CertificateFilename);
        File pkcs8PrivateKeyFile = new File(pkcs8PrivateKeyFilename);

        try {
            RSAPublicKey publicKey = (RSAPublicKey) CryptoUtil.readPublicKeyFromCertificate(x509CertificateFile);
            RSAPrivateKey privateKey = (RSAPrivateKey) CryptoUtil.readPrivateKey(pkcs8PrivateKeyFile);
            Algorithm algorithm = Algorithm.RSA384(publicKey, privateKey);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(iss)
                    .build();
            verifier.verify(token);
            return true;

        } catch (Exception e) {
            logger.warn("caught " + e.getClass().getName() + " validating JWT - " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * generate an Epic access token per specifications documented at
     * https://apporchard.epic.com/Article?docId=oauth2&section=Backend-Oauth2_Getting-Access-Token
     * @param tokenAuthUrl
     * @param jwt
     * @return
     */
    public AccessToken getAccessToken(String tokenAuthUrl, String jwt) throws IOException {
        if ( ! isJWTEnabled() ) return null;

        logger.debug("requesting JWT access token from tokenAuthUrl=" + tokenAuthUrl + ", jwt=" + jwt);

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        params.add(new BasicNameValuePair("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
        params.add(new BasicNameValuePair("client_assertion", jwt));
        String requestBody = URLEncodedUtils.format(params, StandardCharsets.UTF_8);

        HttpResponse httpResponse = new HttpRequest().post(tokenAuthUrl, null, requestHeaders, requestBody);

        int code = httpResponse.getResponseCode();
        String responseBody = httpResponse.getResponseBody();

        if (code < 200 || code > 299) {
            logger.error("received non-successful response to request for a JWT access token for tokenAuthUrl=" + tokenAuthUrl + " with code " + code);
            logger.debug("requestBody=" + requestBody);
            logger.debug("responseBody=" + responseBody);
            throw new MyHttpException(code, responseBody);

        } else {
            Gson gson = new GsonBuilder().create();
            AccessToken accessToken = gson.fromJson(responseBody, new TypeToken<AccessToken>() {}.getType());
            logger.debug("received access token " + accessToken);
            return accessToken;
        }
    }


////////////////////////////////////////////////////////////////////////////
// private methods
//

    private Instant buildExpiresAt() {
        return LocalDateTime.now()
                .plusMinutes(5)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }
}
