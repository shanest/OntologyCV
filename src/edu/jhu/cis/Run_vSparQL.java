/**
 * 
 */
package edu.jhu.cis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author shanest
 * 
 */
public class Run_vSparQL {

	//not in project b/c 225 MB
	private static final String FMA_FILE = "../fma_v3.0.owl";
	private static final String QUERY_FILE = "ontology/query/CVQuery-Eclipse";
	private static final String OUT_FILE = "ontology/Myocardium-20100810.owl";
	private static Model fma_model;

	public static void main(String[] args) {

		fma_model = readOntologyFromFile(FMA_FILE);
		
		String vSparQLString = readQueryString(QUERY_FILE);
		
		Model results = runConstructQuery(fma_model, vSparQLString);
		
		writeModel(results, OUT_FILE);
		
		//ResultSetFormatter.out(System.out, runSelectQuery(selectFile));

	}

	public static void writeModel(Model results, String fname) {
		OutputStream outfile = null;
		try {
			outfile = new FileOutputStream(fname);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		results.write(outfile);
	}

	public static Model runConstructQuery(Model model, String vSparQLString) {
		Query vSparQLQuery = QueryFactory.create(vSparQLString);
		QueryExecution vSparQLqe = QueryExecutionFactory.create(vSparQLQuery,
				model);
		return vSparQLqe.execConstruct();
	}
	
	public static ResultSet runSelectQuery(Model model, String vSparQLString) {
		Query vSparQLQuery = QueryFactory.create(vSparQLString);
		QueryExecution vSparQLqe = QueryExecutionFactory.create(vSparQLQuery,
				model);
		return vSparQLqe.execSelect();
	}

	public static Model readOntologyFromFile(String fmaFile) {
		InputStream in = null;
		try {
			in = new FileInputStream(new File(fmaFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Model fma = ModelFactory.createMemModelMaker().createModel(null);
		fma.read(in, "http://sig.biostr.washington.edu/fma3.0");
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return fma;
	}

	public static String readQueryString(String fname) {
		String line = null, query = "";
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fname));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			while((line = in.readLine()) != null)
				query = query.concat(line + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("query:");
		System.out.println(query);
		
		return query;
	}
}
