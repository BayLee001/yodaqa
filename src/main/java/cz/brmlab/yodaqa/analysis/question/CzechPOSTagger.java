package cz.brmlab.yodaqa.analysis.question;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import cz.brmlab.yodaqa.provider.PrivateResources;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CzechPOSTagger extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CzechPOSTagger.class);
	private static String URL_STRING;

	private static class Response {
		@SerializedName("lemmas")
		List<String> lemmas;
		@SerializedName("pos_tags")
		List<String> posTags;
		@SerializedName("diacritics")
		List<String> diacritics;
	}

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		URL_STRING = PrivateResources.getInstance().getResource("neco");
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		logger.debug("Czech pos tagger");
		Collection<ROOT> sentences = JCasUtil.select(jCas, ROOT.class);
		List<Token> allTokens = new ArrayList<>();
		for(ROOT sentence: sentences) {
			logger.debug("SEP");
			List<Token> tokens = JCasUtil.selectCovered(Token.class, sentence);
			for (Token tok : tokens) {
				logger.debug("TOKEN " + tok.getLemma().getValue());
				logger.debug("POS " + tok.getPos().getPosValue());
			}
			logger.debug("JSON " + createRequest(tokens));
		}
//		String jsonRequest = createRequest(allTokens);
//		InputStream is = sendRequest(jsonRequest);
//		Response response = processResponse(is);
//		addTagToTokens(jCas, allTokens, response);
	}

	private String createRequest(List<Token> tokens) {
		JsonArray jArray = new JsonArray();
		for(Token tok: tokens) {
			jArray.add(tok.getLemma().getValue());
		}
		return jArray.toString();
	}

	public InputStream sendRequest(String json) {
		URL url;
		HttpURLConnection conn = null;
		try {
			url = new URL(URL_STRING);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			OutputStream os = conn.getOutputStream();
			os.write(json.getBytes());
			os.flush();
			return conn.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.disconnect();
			} catch (NullPointerException e1) {}
		}
		return null;
	}

	private Response processResponse(InputStream is) {
		Gson gson = new Gson();
		JsonReader reader = new JsonReader(new InputStreamReader(is));
		reader.setLenient(true);
		return gson.fromJson(reader, Response.class);
	}

	private void addTagToTokens(JCas jCas, List<Token> tokens, Response response) {
		//FIXME Add list size check
		for (int i = 0; i < tokens.size(); i++) {
			POS pos = new POS(jCas);
			Lemma lemma = new Lemma(jCas);
			pos.setPosValue(response.posTags.get(i));
			lemma.setValue(response.lemmas.get(i));
			Token tok = tokens.get(i);
			tok.setLemma(lemma);
			tok.setPos(pos);
		}
	}

}