# Paska - Automated Smell Detection and Recommendation in Requirements Using NLP and CNL
Paska is an automated tool that addresses common quality problems in NL requirements. Paska aims at assisting analysts with automatically detecting quality problems and suggesting recommendations to fix them in functional requirements.

## Prerequisites:
- Java Runtime Environment 1.8.
- A Python 3.8 environment with libraries: allennlp 2.10.1 and allennlp-models 2.10.1. The allennlp library is not supported on Windows. In the folder "config-file", you will find the yaml file "environment.yml" that contains the required python libraries for using Paska.
- Input: A csv text file containing requirements. Each requirement should have the following fields separated by the separator ";":
  - Requirement ID (String)  
  - Requirement (String)

Example:
"RQSVV.024";"If System-A has successfully performed all the validation rules, then System-A must set the state of the Settlement Request to “Valid”."
More examples of requirements can be found in the file "input-requirements.csv" located in the folder "examples/input-requirements".

## Steps to use Paska:
1. Copy the Python file "get_cparsingtrees.pyc" to your local machine. This file contains preprocessing algorithms, such as the generation of the constituency parsing trees.
2. Open the command line; within your Python environment, run the following command passing as arguments the input and output folders:
```python
python get_cparsingtrees.pyc <input-folder> <output-folder>
```
For example, you can user our example folders:
```python
 python get_cparsingtrees.py "/example/input-requirements/" "/example/parsing-trees/"
```
This step may take some time, depending on the number of requirements. For example, using a computer with 16 GB of ram and a corei7 processor, a file with 190 requirements took approximately 4 minutes.
3. You must make sure that you have Java version 1.8 installed. If you have multiple java installations, set Java version 1.8 (JDK) as default.
4. Once you have generated the parsing trees (Step 2), copy to your local machine the smell-detector.jar file, download the POS tagger model "english-left3words-distsim.tagger" from the following url: https://nlp.stanford.edu/software/tagger.shtml#Download . Open the command line; within your JAVA 8 environment, execute the following command passing as arguments the input, output folders, and the folder containing the POS tagger model:
```java
java -jar smell-detector.jar <input-folder-parsing-trees> <output-folder> <folder-postagger-model>
```
For example:
```java
java -jar smell-detector.jar  "/example/parsing-trees/" "/example/output-smells/" "/model/english-left3words-distsim.tagger"
```
5. As a result of Step 4, you will have a csv file containing your requirements along with the detected smells and the suggested Rimay patterns.
For example, the folder "example/output-smells" contains the file "smell.csv".  This file contains the smells detected and suggested Rimay patterns for the requirement examples stored in the "input-requirements.csv" file in the "example/input-requirements" folder.

## Examples of requirements that have smells:

```
R1: When the Settlement Request does not contain all of the mandatory fields, then System-A must immediately return a rejection message to System-B and stop processing this Settlement Request.
R1 has more than one action in the system response.

R2: When System-A validates the order and  if the order is identified as “template”, or if the field XT has the Option F, and if the Qualifier <> EF or the Data Source Scheme <> OP or the Account Type Code is missing or 4 characters, then System-A must push the order in the error queue.
R2 contains multiple conditions separated by conjunction OR.

R3: When System-A receives a confirmation from the System-B with the element Value where value-1 or value-2 present, then in the System-C the information provided in the element must be shown as “Dt” field.
The Verb is missing in the second condition of R3.

More examples can be found in the folder "example/output-smells" file "smell.csv"/"smells.xlsx".

```

## Source code
You can find the source code of the project under the folder "/source".  This folder consists of three projects implemented using Java 8 and Python 3.  Information on the source code is available in the file /source/README.md.

## License:
This software is © University of Luxembourg and is licensed under the GPLv3 license. See LICENSE.
