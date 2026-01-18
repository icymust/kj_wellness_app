package com.ndl.numbers_dont_lie.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Service
public class TwoFactorService {
  private final UserRepository users;
  private final ObjectMapper om;

  @Value("${app.security.issuer:NDL}")
  private String issuer;

  public TwoFactorService(UserRepository users, ObjectMapper om) {
    this.users = users; this.om = om;
  }

  public Map<String, Object> enroll(String email) {
    UserEntity u = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    String secret = Base32.random();
    u.setTotpSecret(secret);
    u.setTwoFactorEnabled(false);
    u.setTwoFactorVerifiedAt(null);
    users.save(u);

    String otpauth = "otpauth://totp/" + urlEncode(issuer) + ":" + urlEncode(email) +
      "?secret=" + secret + "&issuer=" + urlEncode(issuer) + "&digits=6&period=30";
    String qr = toPngBase64(otpauth, 256);

    Map<String,Object> resp = new LinkedHashMap<>();
    resp.put("otpauthUri", otpauth);
    resp.put("secretMasked", mask(secret));
    resp.put("qrPngBase64", qr);
    return resp;
  }

  public Map<String, Object> verifySetup(String email, String code) {
    UserEntity u = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    requireSecret(u);
    if (!verifyCode(u.getTotpSecret(), code)) throw new IllegalStateException("invalid_code");

    u.setTwoFactorEnabled(true);
    u.setTwoFactorVerifiedAt(Instant.now());
    List<String> recovery = generateRecoveryCodes(8);
    try { u.setRecoveryCodesJson(om.writeValueAsString(recovery)); } catch (Exception ignored) {}
    users.save(u);

    return Map.of("enabled", true, "recoveryCodes", recovery);
  }

  public Map<String, Object> disable(String email, String codeOrRecovery) {
    UserEntity u = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    if (!u.isTwoFactorEnabled()) return Map.of("enabled", false);

    boolean ok = false;
    if (u.getTotpSecret() != null && verifyCode(u.getTotpSecret(), codeOrRecovery)) ok = true;
    if (!ok) {
      List<String> rc = parseRecovery(u.getRecoveryCodesJson());
      if (rc.contains(codeOrRecovery)) {
        rc.remove(codeOrRecovery);
        try { u.setRecoveryCodesJson(om.writeValueAsString(rc)); } catch (Exception ignored) {}
        ok = true;
      }
    }
    if (!ok) throw new IllegalStateException("invalid_code");

    u.setTwoFactorEnabled(false);
    u.setTwoFactorVerifiedAt(null);
    u.setTotpSecret(null);
    u.setRecoveryCodesJson(null);
    users.save(u);
    return Map.of("enabled", false);
  }

  public boolean verifyForLogin(String email, String code) {
    UserEntity u = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    requireSecret(u);
    return verifyCode(u.getTotpSecret(), code);
  }

  /**
   * Consume a recovery code for login. Returns true if accepted and removed.
   */
  public boolean useRecoveryForLogin(String email, String recovery) {
    if (recovery == null || recovery.isBlank()) return false;
    UserEntity u = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    java.util.List<String> list = parseRecovery(u.getRecoveryCodesJson());
    if (!list.contains(recovery)) return false;
    list.remove(recovery); // one-time use
    try { u.setRecoveryCodesJson(om.writeValueAsString(list)); } catch (Exception ignored) {}
    users.save(u);
    return true;
  }

  private static String mask(String s) {
    if (s == null || s.length() < 4) return "****";
    return s.substring(0, 4) + "••••";
  }

  private static String urlEncode(String s) {
    try { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
    catch (Exception e) { return s; }
  }

  private static boolean verifyCode(String secret, String code) {
    if (code == null || !code.matches("\\d{6}")) return false;
    Totp totp = new Totp(secret);
    try { return totp.verify(code); } catch (Exception e) { return false; }
  }

  private static List<String> generateRecoveryCodes(int n) {
    Random r = new Random();
    List<String> list = new ArrayList<>();
    for (int i=0;i<n;i++) list.add(randomCode(r));
    return list;
  }

  private static String randomCode(Random r) {
    String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // без похожих символов
    StringBuilder sb = new StringBuilder();
    for (int i=0;i<10;i++) sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
    return sb.toString();
  }

  private static String toPngBase64(String text, int size) {
    try {
      Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.MARGIN, 1);
      QRCodeWriter writer = new QRCodeWriter();
      BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints);
      BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(img, "png", baos);
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (WriterException | java.io.IOException e) {
      return null;
    }
  }

  private static void requireSecret(UserEntity u) {
    if (u.getTotpSecret() == null || u.getTotpSecret().isBlank()) throw new IllegalStateException("no_secret");
  }

  private List<String> parseRecovery(String json) {
    if (json == null || json.isBlank()) return new ArrayList<>();
    try { return om.readValue(json, new TypeReference<List<String>>(){}); }
    catch (Exception e) { return new ArrayList<>(); }
  }
}
