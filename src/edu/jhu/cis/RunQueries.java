/**
 * 
 */
package edu.jhu.cis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author shanest
 * 
 */
public class RunQueries {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		InputStream in = null;
		try {
			in = new FileInputStream(new File(
					"/cis/home/shanest/PURA/fma_v3.0.owl"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Model fma_model = ModelFactory.createMemModelMaker().createModel(null);
		fma_model.read(in, "http://sig.biostr.washington.edu/fma3.0");
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String queryString = "PREFIX fma:<http://sig.biostr.washington.edu/fma3.0#>"
				+ "CONSTRUCT "
				+ "{"
				+ "fma:Heart  fma:regional_part ?object ."
				+ "fma:Heart fma:FMAID ?heart_id ."
				+ "?object fma:FMAID ?id ."
				+ "}"
				+ "WHERE"
				+ "{"
				+ "fma:Heart  fma:regional_part ?object ."
				+ "?object fma:FMAID ?id ."
				+ "fma:Heart fma:FMAID ?heart_id ."
				+ "}";

		queryString = "PREFIX fma:<http://sig.biostr.washington.edu/fma3.0#>"
				+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>"
				+ "CONSTRUCT {" + "?s rdfs:subclassOf ?o ." + "} WHERE {"
				+ "?s rdfs:subclassOf fma:Myocardium_of_region_of_ventricle"
				+ "}";

		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, fma_model);
		Model results = qe.execConstruct();

		results.write(System.out);

		qe.close();

		String selectQueryString = "PREFIX fma:<http://sig.biostr.washington.edu/fma3.0#>"
				+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>"
				+ "SELECT ?s"
				+ "WHERE {"
				+ "?s rdfs:subClassOf fma:Myocardium_of_region_of_ventricle"
				+ "}";

		Query querySelect = QueryFactory.create(selectQueryString);
		QueryExecution qe2 = QueryExecutionFactory.create(querySelect,
				fma_model);
		ResultSet resultsSelect = qe2.execSelect();

		ResultSetFormatter.out(System.out, resultsSelect);

		qe2.close();

	}

}
