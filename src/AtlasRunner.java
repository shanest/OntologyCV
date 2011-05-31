import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

import edu.jhu.cis.Predicate;
import edu.jhu.cis.Run_vSparQL;

/**
 * 
 * Everything else I've used is in the edu.jhu.cis package, but because niftijlib is in the default package,
 * I cannot access the Nifti1Dataset class from edu.jhu.cis.  I could move the source file for niftijlib
 * into my package, but that seems less ideal than making my runner exist in the default package.
 * 
 */

/**
 * @author shanest
 * 
 */
public class AtlasRunner {

	private static final String MYOCARDIUM_OWL = "ontology/Myocardium-20100810.owl";
	private static final String ATLAS_DIR = "../HumanTemplate/Redux/NoResampleMapping/";
	private static final String ATLAS_VOL = "NewAtlasDef004_anal_32bit_flipvert.img"; //converting to ANALYZE did not affect results, i.e. data array the same
	//private static final String T_VOL = "snpmTneg_flip.img";
	private static final String T_VOL = "snpmTnegflipMasked.img";
	private static final String ROI_FILE = "ROI_significant_imageFWE-.img";
	
	private static Nifti1Dataset HUMAN_LV_ATLAS = null;
	private static Nifti1Dataset LV_T = null;
	private static Nifti1Dataset ROI = null;
	// NOTE: [Z][Y][X] array
	private static double[][][] LV_ATLAS_DATA = null;
	private static double[][][] LV_T_DATA = null;
	private static double[][][] ROI_DATA = null;
	
	//from aux_file
	private static String LABEL_FILE = null;
	//1:1 map from Integer (Region) -> Label
	private static BidiMap LABELS = null;

	public static void main(String[] args) {
		
		//Reorienting deleted header info, restore here
		//fixAtlasVolume();
		
		/**
		 * PRELIMS / SETUP
		 */
		readAtlasVolume();
		readTVolume();
		readROIVolume();

		//if (HUMAN_LV_ATLAS.intent_code == Nifti1Dataset.NIFTI_INTENT_LABEL) {
			//I obviously know the intent is LABEL, but this "if" is good practice
			
			if(HUMAN_LV_ATLAS.aux_file == null) {
				System.err.println("Atlas intent is Labels, but no label file specified in aux_file. Aborting.");
				System.exit(0);
			} else {	
				populateLabels();
			}
		//}
		
		/**
		 * RESULTS
		 */
		
		//Result 1: which regions of myocardium are labelled in the atlas?
		//List<String> containedRegions = findLabelledRegions();
		//System.out.println(containedRegions.size() + ": " + containedRegions);
		
		//RESULT: average T-values by region
		Map<String, Float> avgTByRegion = avgTRegions();
		
		//RESULT: where is ROI located?
		Map<String, Integer> regionsOfInterest = countStrings(listROIMyocardium());

		/**
		 * TESTING

		
		double[][][] myocard_17 = filterByPredicate(LV_ATLAS_DATA, new Predicate<Double>() {
			
			public boolean predicate(Double v) {
				return v == 17;
			}
			
		});
		//test that filter worked
		System.out.println(myocard_17[128][203][162]);
		System.out.println(myocard_17[56][145][129]);
		
		//getKey: will be used in conjunction with results from FMA Query
		System.out.println(LABELS.getKey("http://sig.biostr.washington.edu/fma3.0#Myocardial_zone_13"));
		System.out.println(LV_T_DATA[128][180][133]);
		*/

	}

	private static Map<String, Float> avgTRegions() {
		
		Map<Integer, Integer> numVoxelsPerRegion = new HashMap<Integer, Integer>();
		Map<Integer, Float> avgPerRegion = new HashMap<Integer, Float>();
		
		for(int seg = 0; seg < LABELS.size(); seg++) {
			int numVoxels = 0;
			float sum = 0;
			final int segidx = 10*(seg + 1); //10 b/c intensities in this atlas are mult by 10
			
			double[][][] curSeg = filterByPredicate(LV_ATLAS_DATA, new Predicate<Double>() {
				
				public boolean predicate(Double v) {
					return v == segidx;
				}
				
			});
			
			//NOTE these segments don't fully partition the deformed Atlas, b/c there are "transitions" w/ intensities in between these 17
				
			for(int i = 0; i < curSeg.length; i++)
				for(int j = 0; j < curSeg[i].length; j++)
					for(int k = 0; k < curSeg[i][j].length; k++) {
						if(curSeg[i][j][k] != 0 && LV_T_DATA[i][j][k] != 0 /* && !((new Double(Double.NaN)).equals(new Double(LV_T_DATA[i][j][k]))) */) {
							//TODO Figure out why some regions never meet the above, simple criterion of overlap
							numVoxels++;
							sum += LV_T_DATA[i][j][k];
							//if((new Double(Double.NaN).equals(new Double(LV_T_DATA[i][j][k]))))
							//System.out.println("Data: " + LV_T_DATA[i][j][k] + ", new sum: " + sum);
						}
					}
			
			System.out.println("Seg: " + seg + ", sum: " + sum + ", numVoxels: " + numVoxels);
			numVoxelsPerRegion.put(seg, numVoxels);
			avgPerRegion.put(seg, sum / numVoxels);
		}
		
		System.out.println("avgPerRegion");
		System.out.println(avgPerRegion);
		Map<String, Float> namedAvg = new HashMap<String, Float>();
		for(int i = 0; i < avgPerRegion.size(); i++)
			//TODO refactor all the mults by 10 out to a static field
			namedAvg.put((String) LABELS.get((new Integer(10*(i+1))).doubleValue()), avgPerRegion.get(i));
		
		System.out.println(namedAvg);
		return namedAvg;
	}

	/**
	 * Returns a 17 x 256 x 256 x 256 array where resegment()[i] is the ith region of myocardium only.
	 * 
	 * @return a HUGE (be weary heap errors) array
	 */
	private static double[][][][] resegment() {
		double[][][][] segments = new double[LABELS.size()][LV_ATLAS_DATA.length][LV_ATLAS_DATA.length][LV_ATLAS_DATA.length];
		
		for(int i = 0; i < 17; i++) {
			final int ii = 10*i;
			segments[i] = filterByPredicate(LV_ATLAS_DATA, new Predicate<Double>() {
				
				public boolean predicate(Double v) {
					return v == ii;
				}
				
			});
		}
		
		return segments;
	}

	private static void fixAtlasVolume() {
		Nifti1Dataset temp_atlas = new Nifti1Dataset(ATLAS_DIR + ATLAS_VOL);
		
		try {
			temp_atlas.readHeader();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		temp_atlas.intent_code = Nifti1Dataset.NIFTI_INTENT_LABEL;
		temp_atlas.aux_file = new StringBuffer("HeartAtlasLabels.txt");
		temp_atlas.descrip = new StringBuffer("Full LV myocardium atlas");
		
		byte[] temp_data = null;
		try {
			temp_data = temp_atlas.readData();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			temp_atlas.writeHeader();
			temp_atlas.writeData(temp_data);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static Map<String, Integer> countStrings(List<String> list) {
		List<String> allVoxels = list;
		Map<String, Integer> numVoxels = new HashMap<String, Integer>();
		
		int count = 0;
		for(int i = 0; i < allVoxels.size() - 1; i++)
			if(allVoxels.get(i).equals(allVoxels.get(i+1))) {
				count++;
				if(i == allVoxels.size() - 2)
					//add the last item
					numVoxels.put(allVoxels.get(i), count);
			} else {
				numVoxels.put(allVoxels.get(i), count);
				count = 0;
			}
		
		//only one string in allVoxels
		if(numVoxels.size() == 0)
			numVoxels.put(allVoxels.get(0), count);
		
		System.out.println(numVoxels);
		return numVoxels;
	}

	/**
	 * Generates a list of all the regions of myocardium in the ROI image.
	 * Adds the label to list for every voxel, so that it can be counted how many voxels are in diff labelled regions.
	 * 
	 * @return the aforementioned list
	 */
	private static List<String> listROIMyocardium() {
		List<String> results = new ArrayList<String>();
		
		int roiNotInAtlas = 0, totalROI = 0;
		
		for(int i = 0; i < ROI_DATA.length; i++)
			for(int j = 0; j < ROI_DATA[i].length; j++)
				for(int k = 0; k < ROI_DATA[i][j].length; k++)
					if(ROI_DATA[i][j][k] != 0)
					{
						totalROI++;
						if(LV_ATLAS_DATA[i][j][k] == 0)
							roiNotInAtlas++;
						else
						{
							//System.out.println(LV_ATLAS_DATA[i][j][k]);
							//System.out.println(LV_ATLAS_DATA[i][j][k] + ": " + (String) LABELS.get(LV_ATLAS_DATA[i][j][k]));
							if(LV_ATLAS_DATA[i][j][k] % 10.0 == 0) { //if an integer
								//System.out.println("ROI in Atlas with intensity " + LV_ATLAS_DATA[i][j][k]);
								results.add((String) LABELS.get(LV_ATLAS_DATA[i][j][k]));
							}
						}
					}
		
		System.out.println("Total ROI Voxels: " + totalROI);
		System.out.println("ROI Voxels not in Atlas: " + roiNotInAtlas);
		Collections.sort(results, new MyocardStringComparator());
		//System.out.println(results);
		return results;
	}

	private static List<String> findLabelledRegions() {
		List<String> results = new ArrayList<String>();
		
		//read in the myocardium regions
		Model myocardium = Run_vSparQL.readOntologyFromFile(MYOCARDIUM_OWL);
		ResultSet regions = Run_vSparQL.runSelectQuery(myocardium, Run_vSparQL.readQueryString("ontology/query/SubregionsMyocardium"));
		//ResultSetFormatter.out(regions);
		
		QuerySolution region = null;
		String regionStr = "";
		while(regions.hasNext()) {
			region = regions.next();
			regionStr = region.get("?x").toString();
			if(LABELS.containsValue(regionStr))
				results.add(regionStr);
		}
		
		Collections.sort(results, new MyocardStringComparator());
		
		return results;
	}
	
	/**
	 * PRELIMINARY HELPERS
	 */

	//TODO think about re-making a Filterable3DDoubleArray class...
	private static double[][][] filterByPredicate(double[][][] data,
			Predicate<Double> P) {
		double[][][] local = copyDouble3DArray(data);
		for(int i = 0; i < local.length; i++)
			for(int j = 0; j < local[i].length; j++)
				for(int k = 0; k < local[i][j].length; k++)
					if(P.predicate((Double) local[i][j][k]) == false)
						local[i][j][k] = 0;
		return local;
	}
	
	private static double[][][] copyDouble3DArray(double[][][] source) {
		double[][][] destination = new double[source.length][source[0].length][source[0][0].length];
		
		for(int i = 0; i < source.length; i++)
			for(int j = 0; j < source[0].length; j++)
				System.arraycopy(source[i][j], 0, destination[i][j], 0, source[i][j].length);
		
		return destination;
	}

	private static Nifti1Dataset readVolume(String file) {
		Nifti1Dataset nifti = new Nifti1Dataset(file);
		try {
			nifti.readHeader();
			nifti.printHeader();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return nifti;
	}

	private static void readTVolume() {
		LV_T = readVolume(ATLAS_DIR + T_VOL);

		try {
			LV_T_DATA = LV_T.readDoubleVol((short) 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void readAtlasVolume() {
		HUMAN_LV_ATLAS = readVolume(ATLAS_DIR + ATLAS_VOL);

		try {
			LV_ATLAS_DATA = HUMAN_LV_ATLAS.readDoubleVol((short) 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void readROIVolume() {
		ROI = readVolume(ATLAS_DIR + ROI_FILE);

		try {
			ROI_DATA = ROI.readDoubleVol((short) 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void populateLabels() {
		//LABEL_FILE = ATLAS_DIR + HUMAN_LV_ATLAS.aux_file.toString();
		LABEL_FILE = ATLAS_DIR + "NewAtlasLabels.txt";
		LABELS = new TreeBidiMap();
		
		Scanner scan = null;
		
		try {
			scan = new Scanner(new BufferedReader(new FileReader("../HumanTemplate/" + LABEL_FILE)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		while (scan.hasNext())
			LABELS.put(scan.nextDouble(), scan.next());

		if (scan != null)
			scan.close();

	}
	

	private static final class MyocardStringComparator implements
			Comparator<String> {
		//Myocardial_zone_1, Myocardial_zone_2, Myocardial_zone_3, ...
		@Override
		public int compare(String region1, String region2) {
			return (int) (new Double(LABELS.getKey(region1).toString()) - new Double(LABELS.getKey(region2).toString()));
		}
	}

}
