import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;

import edu.jhu.cis.Predicate;

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

	private static final String ATLAS_DIR = "../HumanTemplate/";
	private static final String ATLAS_VOL = "HeartAtlasFull.nii";
	private static final String T_VOL = "ES2Atlas_4/snpmTneg_7-FSLAffine_def003.img";

	private static Nifti1Dataset HUMAN_LV_ATLAS = null;
	private static Nifti1Dataset LV_T = null;
	// NOTE: [Z][Y][X] array
	private static double[][][] LV_ATLAS_DATA = null;
	private static double[][][] LV_T_DATA = null;
	
	//from aux_file
	private static String LABEL_FILE = null;
	//1:1 map from Integer (Region) -> Label
	private static BidiMap LABELS = null;

	public static void main(String[] args) {
		
		readAtlasVolume();
		readTVolume();

		if (HUMAN_LV_ATLAS.intent_code == Nifti1Dataset.NIFTI_INTENT_LABEL) {
			//I obviously know the intent is LABEL, but this "if" is good practice
			
			if(HUMAN_LV_ATLAS.aux_file == null) {
				System.err.println("Atlas intent is Labels, but no label file specified in aux_file. Aborting.");
				System.exit(0);
			}
				
			populateLabels();
		}
		
		double[][][] myocard_17 = filterByPredicate(LV_ATLAS_DATA, new Predicate<Double>() {
			
			public boolean predicate(Double v) {
				return v == 17;
			}
			
		});
		
		System.out.println(LABELS.get(LV_ATLAS_DATA[128][203][162]));
		
		//test that filter worked
		System.out.println(myocard_17[128][203][162]);
		System.out.println(myocard_17[56][145][129]);
		
		//getKey: will be used in conjunction with results from FMA Query
		System.out.println(LABELS.getKey("http://sig.biostr.washington.edu/fma3.0#Myocardial_zone_13"));
		System.out.println(LV_T_DATA[128][180][133]);

	}

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

	/**
	 * PRELIMINARY HELPERS
	 */

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

	private static void populateLabels() {
		LABEL_FILE = HUMAN_LV_ATLAS.aux_file.toString();
		LABELS = new TreeBidiMap();
		
		Scanner scan = null;
		
		try {
			scan = new Scanner(new BufferedReader(new FileReader(ATLAS_DIR + LABEL_FILE)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		while (scan.hasNext())
			LABELS.put(scan.nextDouble(), scan.next());

		if (scan != null)
			scan.close();

	}

}
