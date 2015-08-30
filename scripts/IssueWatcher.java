package com.sap.authoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sap.prd.access.credentials.api.Credential;
import com.sap.prd.access.credentials.api.ProdPassAccess;

//TestWatchman is used for compatibility purpose with older versions of JUnit
@SuppressWarnings("deprecation")
public class IssueWatcher extends TestWatchman {

	public static void main(String[] args) {
		try {
			Issue issue = new CWBIssue("012006153200001159182015");
			
			issue = new JiraIssue("https://sapjira.wdf.sap.corp/browse/BITBIWEBISL2-1542");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				String issueCheck = System.getProperty("issue.check");
				Throwable testError = null;
				Defect defect = (issueCheck == null) ? null : method.getAnnotation(Defect.class);

				Issue issue = null;
				if (defect != null) {
					if (defect.type().equals("jira")) {
						issue = new JiraIssue(defect.url());
					} else if (defect.type().equals("cwb")) {
						issue = new CWBIssue(defect.url());
					}
				}

				// Run the test
				starting(method);
				try {
					base.evaluate();
				} catch (Throwable t) {
					testError = t;
				}

				boolean success = false;
				if (issue != null) {
					success = testError == null;
					// if the test failed
					if (!success) {
						boolean errorMessageMatches = matches(testError.getMessage(), defect.message());
						if (errorMessageMatches && "showDefects".equals(issueCheck)) {
							testError = new Exception("The defect '" + issue.getBrowseLink() + "' is associated to this test. Its status is '"
									+ issue.getStatus() + "'");
						} else {
							success = errorMessageMatches && issue.isOpen();
						}
					}

					// Log actual result
					System.out.println("\n*** issueWatcher - test '" + method.getName() + "' ***");
					System.out.println(" - Defect " + issue.getBrowseLink() + ": " + issue.getStatus());
					System.out.println(" - Actual test status: " + ((testError == null) ? "passed" : "failed"));
					if (testError != null) {
						System.out.println(" - Expected error message: " + defect.message());
						System.out.println(" - Actual error message: " + testError.getMessage());
					}
					System.out.println(" - Final test status: " + (success ? "passed" : "failed"));
					System.out.println("*** JiraIssueWatcher ***");
				}
				try {
					if (success) {
						succeeded(method);
					} else {
						failed(testError, method);
						throw testError;
					}
				} finally {
					finished(method);
				}
			}
		};
	}

	/**
	 * @param url
	 *            Browse URL of the defect (e.g. <i>https://jira.atlassian.com/browse/TST-51499</i><br>
	 *            or<br>
	 *            the Correction Request id if the type is cwb).
	 * @param message
	 *            Exact text or regular expression matching the message of the exception raised by the test. The default value matches all messages.
	 *
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Defect {
		String url();

		String message() default ".*";

		String type() default "jira";
	}

	private boolean matches(String toMatch, String pattern) {
		boolean matches = toMatch.equals(pattern);
		if (!matches) {
			Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
			matches = p.matcher(toMatch).find();
		}
		return matches;
	}

	/*
	 * ##########################################################################
	 * 
	 * Specifics inner classes getting status from Jira or Java Correction Workbench
	 * 
	 * ##########################################################################
	 */

	private static class JiraIssue extends IssueAbs implements Issue {
		private String jiraIssueRestUrl, jiraIssueBrowseUrl;
		private String status;

		public JiraIssue(String issueUrl) throws Exception {
			this.jiraIssueBrowseUrl = issueUrl;

			URL url = new URL(issueUrl);
			String baseUrl = url.getProtocol() + "://" + url.getHost() + ((url.getPort() == -1) ? "" : (":" + url.getPort()));
			String issueId = issueUrl.substring(issueUrl.lastIndexOf('/') + 1);
			this.jiraIssueRestUrl = baseUrl + "/rest/api/latest/issue/" + issueId;

			this.status = getIssueStatus(jiraIssueRestUrl);
			System.out.println("Jira status : "+status);
		}

		@Override
		public String getStatus() {
			return this.status;
		}

		@Override
		public String getBrowseLink() {
			return this.jiraIssueBrowseUrl;
		}

		@Override
		public boolean isOpen() {
			return "Open".equals(status) || "Active".equals(status);
		}

		private String getIssueStatus(String issueRestUrl) throws Exception {
			String issue = execute(issueRestUrl);

			String regex = ".*\"status\":\\{.*?\"name\":\"(.*?)\".*?\\}.*";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(issue);

			boolean found = matcher.find();
			if (!found) {
				throw new Exception("Issue status not found for " + issueRestUrl);
			}

			String status = matcher.group(1);

			return status;
		}

		private String execute(String url) throws Exception {
			String strUrl = url;
			StringBuilder buffer = new StringBuilder();

			try {
				HttpsURLConnection connection = sendRequest(strUrl, null, null);

				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String input;
				while ((input = br.readLine()) != null) {
					buffer.append(input);
				}
				br.close();
			} catch (Exception e) {
				throw new Exception("Cannot execute URL '" + url + "': " + e.getMessage(), e);
			}

			return buffer.toString();
		}

	}

	private static class CWBIssue extends IssueAbs implements Issue {

		private String status, crid;

		public CWBIssue(String CRID) throws Exception {
			this.crid = CRID;
			this.status = findStatus(this.crid);
			System.out.println("Status : " + status);
		}

		@Override
		public String getStatus() {
			return this.status;
		}

		@Override
		public String getBrowseLink() {
			return this.crid;
		}

		@Override
		public boolean isOpen() {
			return "Open".equals(status) || "Active".equals(status);
		}

		private String findStatus(String crid2) throws Exception {
//			System.getProperty("user.home")+"//<foldername>//"
			File localFile = new File("C://test//");
			ProdPassAccess access = new ProdPassAccess(localFile);
			Credential credential = access.getCredential("CWB");
			String username = credential.getProperties().getProperty("user");
			String password = credential.getProperties().getProperty("password");

			String strUrl = "https://css.wdf.sap.corp/sap/bc/bsp/spn/jcwb_api_extern/get_correction_requests?pointer=" + crid2;
			HttpsURLConnection con = sendRequest(strUrl, username, password);
			int status = con.getResponseCode();

			if (status == 200) {
				DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = builderFactory.newDocumentBuilder();
				builder.setEntityResolver(new EntityResolver() {

					@Override
					public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
						return new InputSource(new StringReader(""));
					}
				});

				InputStream in = con.getInputStream();
				Document xmlDocument = builder.parse(in);
				in.close();
				XPathFactory xpf = XPathFactory.newInstance();
				XPath xpath = xpf.newXPath();
				NodeList nodeList = (NodeList) xpath.compile("//status").evaluate(xmlDocument, XPathConstants.NODESET);

				return nodeList.item(0).getFirstChild().getNodeValue();

			} else {
				Reader reader = new InputStreamReader(status < 400 ? con.getInputStream() : ((HttpURLConnection) con).getErrorStream());

				System.err.print("ERROR: ");
				while (true) {
					int ch = reader.read();
					if (ch == -1) {
						System.err.println();
						break;
					}
					System.err.print((char) ch);
				}
				return null;
			}

		}

	}

	private interface Issue {
		public String getStatus();

		public String getBrowseLink();

		public boolean isOpen();

	}
}

abstract class IssueAbs {

	private void disableCertificateValidation() throws NoSuchAlgorithmException, KeyManagementException {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(null, trustAllCerts, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}

	public HttpsURLConnection sendRequest(String strUrl, String username, String password) throws KeyManagementException, NoSuchAlgorithmException,
			MalformedURLException, IOException {

		disableCertificateValidation();

		URL url = new URL(strUrl);
		HttpsURLConnection httpsConnexion = (HttpsURLConnection) url.openConnection();

		if (username != null && password != null) {
			String authStr = username + ":" + password;
			String authEncoded = Base64.encodeBase64String(authStr.getBytes());
			httpsConnexion.setRequestProperty("Authorization", "Basic " + authEncoded);
		}

		httpsConnexion.setRequestMethod("GET");

		return httpsConnexion;
	}
}
