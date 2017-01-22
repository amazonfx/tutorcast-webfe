package config;

public class ConfigBean {
  private String awsAccessKey;
  private String awsSecretKey;
  private String s3DocBucket;
  private String s3DocUrl;
  private String orbitHost;
  private String orbitPort;
  private Integer tokKey;
  private String tokSecret;
  private String tokEndpoint;
  private String cloudfrontStreaming;
  private String fbAppID;
  private String fbAppSecret;  
  
  private String linkedinAppID;
  private String linkedinAppSecret;
  
  private String linkedinUserToken;
  private String linkedinUserSecret;
  private String deployedUrl;
  private Integer freeMaxMinutes;
  private Integer paidMaxMinutes;
  
  private String stripePublicKey;
  private String stripeSecretKey;
  
  
  
  public String getStripePublicKey() {
    return stripePublicKey;
  }
  public void setStripePublicKey(String stripePublicKey) {
    this.stripePublicKey = stripePublicKey;
  }
  public String getStripeSecretKey() {
    return stripeSecretKey;
  }
  public void setStripeSecretKey(String stripeSecretKey) {
    this.stripeSecretKey = stripeSecretKey;
  }
  public Integer getFreeMaxMinutes() {
    return freeMaxMinutes;
  }
  public void setFreeMaxMinutes(Integer freeMaxMinutes) {
    this.freeMaxMinutes = freeMaxMinutes;
  }
  public Integer getPaidMaxMinutes() {
    return paidMaxMinutes;
  }
  public void setPaidMaxMinutes(Integer paidMaxMinutes) {
    this.paidMaxMinutes = paidMaxMinutes;
  }
  public String getDeployedUrl() {
    return deployedUrl;
  }
  public void setDeployedUrl(String deployedUrl) {
    this.deployedUrl = deployedUrl;
  }
  public String getLinkedinAppID() {
    return linkedinAppID;
  }
  public void setLinkedinAppID(String linkedinAppID) {
    this.linkedinAppID = linkedinAppID;
  }
  public String getLinkedinAppSecret() {
    return linkedinAppSecret;
  }
  public void setLinkedinAppSecret(String linkedinAppSecret) {
    this.linkedinAppSecret = linkedinAppSecret;
  }
  public String getLinkedinUserToken() {
    return linkedinUserToken;
  }
  public void setLinkedinUserToken(String linkedinUserToken) {
    this.linkedinUserToken = linkedinUserToken;
  }
  public String getLinkedinUserSecret() {
    return linkedinUserSecret;
  }
  public void setLinkedinUserSecret(String linkedinUserSecret) {
    this.linkedinUserSecret = linkedinUserSecret;
  }
  public String getFbAppID() {
    return fbAppID;
  }
  public void setFbAppID(String fbAppID) {
    this.fbAppID = fbAppID;
  }
  public String getFbAppSecret() {
    return fbAppSecret;
  }
  public void setFbAppSecret(String fbAppSecret) {
    this.fbAppSecret = fbAppSecret;
  }
  public String getCloudfrontStreaming() {
    return cloudfrontStreaming;
  }
  public void setCloudfrontStreaming(String cloudfrontStreaming) {
    this.cloudfrontStreaming = cloudfrontStreaming;
  }
  public String getTokEndpoint() {
    return tokEndpoint;
  }
  public void setTokEndpoint(String tokEndpoint) {
    this.tokEndpoint = tokEndpoint;
  }
  public Integer getTokKey() {
    return tokKey;
  }
  public void setTokKey(Integer tokKey) {
    this.tokKey = tokKey;
  }
  public String getTokSecret() {
    return tokSecret;
  }
  public void setTokSecret(String tokSecret) {
    this.tokSecret = tokSecret;
  }
  public String getAwsAccessKey() {
    return awsAccessKey;
  }
  public void setAwsAccessKey(String awsAccessKey) {
    this.awsAccessKey = awsAccessKey;
  }
  public String getAwsSecretKey() {
    return awsSecretKey;
  }
  public void setAwsSecretKey(String awsSecretKey) {
    this.awsSecretKey = awsSecretKey;
  }
  public String getS3DocBucket() {
    return s3DocBucket;
  }
  public void setS3DocBucket(String s3DocBucket) {
    this.s3DocBucket = s3DocBucket;
  }
  public String getS3DocUrl() {
    return s3DocUrl;
  }
  public void setS3DocUrl(String s3DocUrl) {
    this.s3DocUrl = s3DocUrl;
  }
  public String getOrbitHost() {
    return orbitHost;
  }
  public void setOrbitHost(String orbitHost) {
    this.orbitHost = orbitHost;
  }
  public String getOrbitPort() {
    return orbitPort;
  }
  public void setOrbitPort(String orbitPort) {
    this.orbitPort = orbitPort;
  }  
}
