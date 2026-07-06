# Paska-source

# 1. Read Requirements from MS Word documents
The file "/read-word-file/src/main/java/readWordFileSoftPreprocessing.java" contains a method that reads MS Words files and extract requirements. In this file you should update the path to the folder containing the word documents with requirements. The word files should comply to the template used by our industrial partner. The extracted requirements will be stored in the same folder specified below as csv files.

```
public static String pathWordDocuments = "path to folder";
```

## Requirements

- Java 8
- Other dependencies are specified in the pom.xml file

# 2. Generate constituency parsing trees (Python)
The file "/get-parsing-trees/get_cparsingtrees.py" receives as an input the csv file(s) from Step 1. and generates the constituency parsing trees. You should run the python application executing the command below specifying the input-folder and  output-folder.  The input-folder should point to the files generated in Step 1 ( Read Requirements from MS Word documents).

```python
python get_cparsingtrees.py <input-folder> <output-folder>
```

## Requirements
- Python 3
- Additional libraries required to generate constituency parsing trees are specified in the yml file located at 
paska/config-file/environment.yml



# 3. Smells and recommend patterns
The main file "/smell-detector-publication/src/main/java/Approach.java" reads files and analyzes them to find smells and suggest patterns. Please update the paths for the input, output files and Stanford model .

```
String pathCsvDocuments = ""; //Specify the path to the folder that contains the parsing trees 
String csvFolderOut = ""; //Specify the path to the folder that will store the file(s) with detected smells 
String taggerModelPath = ""; //Specify the path to stanford tagger  english-left3words-distsim.tagger you should donwload from the standford web site
```

## Requirements

- Java 8
- Other dependencies are specified in the pom.xml file 
