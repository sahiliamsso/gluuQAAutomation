package org.gluu.gluuQAAutomation.webreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.EmptyReportable;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.ReportParser;
import net.masterthought.cucumber.ReportResult;
import net.masterthought.cucumber.Reportable;
import net.masterthought.cucumber.Trends;
import net.masterthought.cucumber.ValidationException;
import net.masterthought.cucumber.generators.ErrorPage;
import net.masterthought.cucumber.json.Feature;
import net.masterthought.cucumber.json.support.TagObject;

public class QAReportBuilder {

	private static final Logger LOG = Logger.getLogger(ReportBuilder.class.getName());

	/**
	 * Page that should be displayed when the reports is generated. Shared between
	 * {@link FeaturesOverviewPage} and {@link ErrorPage}.
	 */

	/**
	 * Subdirectory where the report will be created.
	 */
	public static final String BASE_DIRECTORY = "cucumber-html-reports";

	public static final String HOME_PAGE = "overview-features.html";

	private static final ObjectMapper mapper = new ObjectMapper();

	private ReportResult reportResult;
	private final ReportParser reportParser;

	private Configuration configuration;
	private List<String> jsonFiles;

	/**
	 * Flag used to detect if the file with updated trends is saved. If the report
	 * crashes and the trends was not saved then it tries to save trends again with
	 * empty data to mark that the build crashed.
	 */
	private boolean wasTrendsFileSaved = false;

	public QAReportBuilder(List<String> jsonFiles, Configuration configuration) {
		this.jsonFiles = jsonFiles;
		this.configuration = configuration;
		reportParser = new ReportParser(configuration);
	}

	/**
	 * Parses provided files and generates the report. When generating process fails
	 * report with information about error is provided.
	 * 
	 * @return stats for the generated report
	 */
	public Reportable generateReports() {
		Trends trends = null;

		try {
			// first copy static resources so ErrorPage is displayed properly
			copyStaticResources();
			moveStaticRessources();
			Files.copy(Paths.get("src/main/resources/favicon.png"),
					Paths.get("src/main/resources/static/images/favicon.png"), StandardCopyOption.REPLACE_EXISTING);
			// create directory for embeddings before files are generated
			// createEmbeddingsDirectory();

			// add metadata info sourced from files
			reportParser.parseClassificationsFiles(configuration.getClassificationFiles());

			// parse json files for results
			List<Feature> features = reportParser.parseJsonFiles(jsonFiles);
			reportResult = new ReportResult(features, configuration.getSortingMethod());
			Reportable reportable = reportResult.getFeatureReport();

			if (configuration.isTrendsStatsFile()) {
				// prepare data required by generators, collect generators and generate pages
				trends = updateAndSaveTrends(reportable);
			}

			// Collect and generate pages in a single pass
			generatePages(trends);
			moveTemplates();

			return reportable;

			// whatever happens we want to provide at least error page instead of incomplete
			// report or exception
		} catch (Exception e) {
			generateErrorPage(e);
			// update trends so there is information in history that the build failed

			// if trends was not created then something went wrong
			// and information about build failure should be saved
			if (!wasTrendsFileSaved && configuration.isTrendsStatsFile()) {
				Reportable reportable = new EmptyReportable();
				updateAndSaveTrends(reportable);
			}

			// something went wrong, don't pass result that might be incomplete
			return null;
		}
	}

	private void copyStaticResources() {
		copyResources("css", "cucumber.css", "bootstrap.min.css", "font-awesome.min.css");
		copyResources("js", "jquery.min.js", "jquery.tablesorter.min.js", "bootstrap.min.js", "Chart.min.js",
				"moment.min.js");
		copyResources("fonts", "FontAwesome.otf", "fontawesome-webfont.svg", "fontawesome-webfont.woff",
				"fontawesome-webfont.eot", "fontawesome-webfont.ttf", "fontawesome-webfont.woff2",
				"glyphicons-halflings-regular.eot", "glyphicons-halflings-regular.eot",
				"glyphicons-halflings-regular.woff2", "glyphicons-halflings-regular.woff",
				"glyphicons-halflings-regular.ttf", "glyphicons-halflings-regular.svg");
		copyResources("images", "favicon.png");
	}

	// private void createEmbeddingsDirectory() {
	// configuration.getEmbeddingDirectory().mkdirs();
	// }

	private void copyResources(String resourceLocation, String... resources) {
		for (String resource : resources) {
			File tempFile = new File(configuration.getReportDirectory().getAbsoluteFile(),
					BASE_DIRECTORY + File.separatorChar + resourceLocation + File.separatorChar + resource);
			// don't change this implementation unless you verified it works on Jenkins
			try {
				FileUtils.copyInputStreamToFile(
						this.getClass().getResourceAsStream("/" + resourceLocation + "/" + resource), tempFile);
			} catch (IOException e) {
				// based on FileUtils implementation, should never happen even is declared
				throw new ValidationException(e);
			}
		}
	}

	private void generatePages(Trends trends) {
		new QAFeaturesOverviewPage(reportResult, configuration).generatePage();

		for (Feature feature : reportResult.getAllFeatures()) {
			new QAFeatureReportPage(reportResult, configuration, feature).generatePage();
		}

		new QATagsOverviewPage(reportResult, configuration).generatePage();

		for (TagObject tagObject : reportResult.getAllTags()) {
			new QATagReportPage(reportResult, configuration, tagObject).generatePage();
		}

		new QAStepsOverviewPage(reportResult, configuration).generatePage();
		new QAFailuresOverviewPage(reportResult, configuration).generatePage();

		if (configuration.isTrendsStatsFile()) {
			new QATrendsOverviewPage(reportResult, configuration, trends).generatePage();
		}
	}

	private Trends updateAndSaveTrends(Reportable reportable) {
		Trends trends = loadOrCreateTrends();
		appendToTrends(trends, reportable);

		// save updated trends so it contains all history
		saveTrends(trends, configuration.getTrendsStatsFile());

		// display only last n items - don't skip items if limit is not defined
		if (configuration.getTrendsLimit() > 0) {
			trends.limitItems(configuration.getTrendsLimit());
		}

		return trends;
	}

	private Trends loadOrCreateTrends() {
		File trendsFile = configuration.getTrendsStatsFile();
		if (trendsFile != null && trendsFile.exists()) {
			return loadTrends(trendsFile);
		} else {
			return new Trends();
		}
	}

	private static Trends loadTrends(File file) {
		try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
			return mapper.readValue(reader, Trends.class);
		} catch (JsonMappingException e) {
			throw new ValidationException(String.format("File '%s' could not be parsed as file with trends!", file), e);
		} catch (IOException e) {
			// IO problem - stop generating and re-throw the problem
			throw new ValidationException(e);
		}
	}

	private void appendToTrends(Trends trends, Reportable result) {
		trends.addBuild(configuration.getBuildNumber(), result);
	}

	private void saveTrends(Trends trends, File file) {
		ObjectWriter objectWriter = mapper.writer().with(SerializationFeature.INDENT_OUTPUT);
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			objectWriter.writeValue(writer, trends);
			wasTrendsFileSaved = true;
		} catch (IOException e) {
			wasTrendsFileSaved = false;
			throw new ValidationException("Could not save updated trends in file: " + file.getAbsolutePath(), e);
		}
	}

	private void generateErrorPage(Exception exception) {
		LOG.log(Level.INFO, "Unexpected error", exception);
		ErrorPage errorPage = new ErrorPage(reportResult, configuration, exception, jsonFiles);
		errorPage.generatePage();
	}

	private void moveStaticRessources() {
		try {
			FileUtils.copyDirectoryToDirectory(Paths.get("src/main/resources/cucumber-html-reports/css").toFile(),
					Paths.get("src/main/resources/static/").toFile());
			FileUtils.copyDirectoryToDirectory(Paths.get("src/main/resources/cucumber-html-reports/js").toFile(),
					Paths.get("src/main/resources/static/").toFile());
			FileUtils.copyDirectoryToDirectory(Paths.get("src/main/resources/cucumber-html-reports/images").toFile(),
					Paths.get("src/main/resources/static/").toFile());
			FileUtils.copyDirectoryToDirectory(Paths.get("src/main/resources/cucumber-html-reports/fonts").toFile(),
					Paths.get("src/main/resources/static/").toFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void moveTemplates() throws IOException {
		FileUtils.copyDirectory(Paths.get("src/main/resources/cucumber-html-reports").toFile(),
				Paths.get("src/main/resources/templates/").toFile());

		Files.copy(Paths.get("src/main/resources/favicon.png"),
				Paths.get("src/main/resources/templates/images/favicon.png"), StandardCopyOption.REPLACE_EXISTING);

		Files.copy(Paths.get("src/main/resources/cucumber-html-reports/overview-failures.html"),
				Paths.get("src/main/resources/templates/FailuresOverviewPage.html"),
				StandardCopyOption.REPLACE_EXISTING);
		Files.copy(Paths.get("src/main/resources/cucumber-html-reports/overview-features.html"),
				Paths.get("src/main/resources/templates/FeaturesOverviewPage.html"),
				StandardCopyOption.REPLACE_EXISTING);
		Files.copy(Paths.get("src/main/resources/cucumber-html-reports/overview-steps.html"),
				Paths.get("src/main/resources/templates/StepsOverviewPage.html"), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(Paths.get("src/main/resources/cucumber-html-reports/overview-tags.html"),
				Paths.get("src/main/resources/templates/TagsOverviewPage.html"), StandardCopyOption.REPLACE_EXISTING);
		FileUtils.deleteDirectory(Paths.get("src/main/resources/templates/js").toFile());
		FileUtils.deleteDirectory(Paths.get("src/main/resources/templates/images").toFile());
		FileUtils.deleteDirectory(Paths.get("src/main/resources/templates/fonts").toFile());
		FileUtils.deleteDirectory(Paths.get("src/main/resources/templates/css").toFile());
	}

}
