import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


public class knn {
	public static void main(String[] args){
//		knn("classification\\glass_train.txt","classification\\glass_test.txt", 2, 0);
//		knn("classification\\glass_train.txt","classification\\glass_test.txt", 2, 1);
//		knn("classification\\glass_train.txt","classification\\glass_test.txt", 2, 2);
//		knn("classification\\glass_train.txt","classification\\glass_test.txt", 2, 2);
//		System.out.println();
//		knn("classification\\glass_train.txt","classification\\glass_test.txt", 1, 0);
//		knn("classification\\glass_train.txt","classification\\glass_test.txt", 6, 1,true);
//		knn("classification\\glass_train.txt","classification\\glass_test.txt", 10, 2,true);
//		knn("classification\\glass_train.txt","classification\\glass_test.txt", 20, 2,true);
//		System.out.println();
//		knn("classification\\dna_train.txt","classification\\dna_test.txt", 4, 0,false);
//		knn("classification\\dna_train.txt","classification\\dna_test.txt", 4, 1,false);
//		knn("classification\\dna_train.txt","classification\\dna_test.txt", 4, 2,false);
//		knn("classification\\dna_train.txt","classification\\dna_test.txt", 15, 2,false);
//		System.out.println();
//		knn("classification\\vowel_train.txt","classification\\vowel_test.txt", 4, 0);
//		knn("classification\\dna_train.txt","classification\\dna_test.txt", 4, 1);
//		knn("classification\\dna_train.txt","classification\\dna_test.txt", 4, 2);
		knn("classification\\dna_train.txt","classification\\dna_test.txt", 15, 2);
		knn("classification\\dna_train.txt","classification\\dna_test.txt", 15, 1);
		knn("classification\\dna_train.txt","classification\\dna_test.txt", 15, 0);
	}
	
	public static void knn(String traningFile, String testFile, int K, int metricType){
		//get the current time
		final long startTime = System.currentTimeMillis();
		
		try {
			//read trainingSet and testingSet
			TrainRecord[] trainingSet =  FileManager.readTrainFile(traningFile);
			TestRecord[] testingSet =  FileManager.readTestFile(testFile);
			
			//determine the type of metric according to metricType
			Metric metric;
			if(metricType == 0)
				metric = new CosineSimilarity();
			else if(metricType == 1)
				metric = new L1Distance();
			else if (metricType == 2)
				metric = new EuclideanDistance();
			else{
				System.out.println("The entered metric_type is wrong!");
				return;
			}
			
			//test those TestRecords one by one
			int numOfTestingRecord = testingSet.length;
			for(int i = 0; i < numOfTestingRecord; i ++){
				TrainRecord[] neighbors = findKNearestNeighbors(trainingSet, testingSet[i], K, metric);
				int classLabel = classify(neighbors);
				testingSet[i].predictedLabel = classLabel; //assign the predicted label to TestRecord
			}
			
			//calculate the accuracy
			int correctPrediction = 0;
			for(int j = 0; j < numOfTestingRecord; j ++){
				if(testingSet[j].predictedLabel == testingSet[j].classLabel)
					correctPrediction ++;
			}
			
			//Output a file containing predicted labels for TestRecords
			String predictPath = FileManager.outputFile(testingSet, traningFile);
			System.out.println("The prediction file is stored in "+predictPath);
			System.out.println("The accuracy is "+(double)correctPrediction / numOfTestingRecord);
			
			//print the total execution time
			final long endTime = System.currentTimeMillis();
			System.out.println("Total excution time: "+(endTime - startTime) / (double)1000 +" seconds.");
		
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	// Find K nearest neighbors of testRecord within trainingSet 
	static TrainRecord[] findKNearestNeighbors(TrainRecord[] trainingSet, TestRecord testRecord,int K, Metric metric){
		int NumOfTrainingSet = trainingSet.length;
		assert K <= NumOfTrainingSet : "K is lager than the length of trainingSet!";
		
		//Update KNN: take the case when testRecord has multiple neighbors with the same distance into consideration
		//Solution: Update the size of container holding the neighbors
		ArrayList<TrainRecord> neighbors = new ArrayList<TrainRecord>();
		
		//initialization, put the first K trainRecords into the above arrayList
		Metric EuclideanDis = new EuclideanDistance();
		
		int index;
		for(index = 0; index < K; index++){
			trainingSet[index].distance = metric.getDistance(trainingSet[index], testRecord);
			if(metric instanceof EuclideanDistance)
				trainingSet[index].EuclideanDistance = trainingSet[index].distance;
			else 
				trainingSet[index].EuclideanDistance = EuclideanDis.getDistance(trainingSet[index], testRecord);
			neighbors.add(trainingSet[index]);
		}
		
		//go through the remaining records in the trainingSet to find K nearest neighbors
		for(index = K; index < NumOfTrainingSet; index ++){
			trainingSet[index].distance = metric.getDistance(trainingSet[index], testRecord);
			if(metric instanceof EuclideanDistance)
				trainingSet[index].EuclideanDistance = trainingSet[index].distance;
			else 
				trainingSet[index].EuclideanDistance = EuclideanDis.getDistance(trainingSet[index], testRecord);
			//get the index of the neighbor with the largest distance to testRecord
			int maxIndex = 0;
			for(int i = 1; i < neighbors.size(); i ++){
				if(neighbors.get(i).distance > neighbors.get(maxIndex).distance)
					maxIndex = i;
			}
			
			//add the current trainingSet[index] into neighbors if applicable
			//append the current trainingSet[index] to neighbors if the distance is equal to the max distance 
			if(neighbors.get(maxIndex).distance > trainingSet[index].distance)
				neighbors.set(maxIndex, trainingSet[index]);
			else if (neighbors.get(maxIndex).distance == trainingSet[index].distance)
				neighbors.add(trainingSet[index]);
		}
		
		return neighbors.toArray(new TrainRecord[neighbors.size()]);
	}
	
	// Get the class label by using neighbors
	static int classify(TrainRecord[] neighbors){
		//construct a HashMap to store <classLabel, weight>
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
		int num = neighbors.length;
		
		for(int index = 0;index < num; index ++){
			TrainRecord temp = neighbors[index];
			int key = temp.classLabel;
			
			//if this classLabel does not exist in the HashMap, put 
			if(!map.containsKey(key))
				map.put(key, 1 / (temp.EuclideanDistance * temp.EuclideanDistance));
			else{
				double value = map.get(key);
				value += 1 / (temp.EuclideanDistance*temp.EuclideanDistance);
				map.put(key, value);
			}
		}	
		
		//Find the most likely label
		double maxSimilarity = 0;
		int returnLabel = -1;
		Set<Integer> labelSet = map.keySet();
		Iterator<Integer> it = labelSet.iterator();
		while(it.hasNext()){
			int label = it.next();
			double value = map.get(label);
			if(value > maxSimilarity){
				maxSimilarity = value;
				returnLabel = label;
			}
		}
		
		return returnLabel;
	}
}
