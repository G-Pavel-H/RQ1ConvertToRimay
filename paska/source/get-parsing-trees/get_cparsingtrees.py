#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Jun  1 10:24:16 2021

@author: alvaro.veizaga@uni.lu
"""

from allennlp.predictors.predictor import Predictor
import spacy

import regex as re
import os
import csv
import sys



def replaceString(x):
    a = x.group(1)
    a = a.strip()
    a = re.sub("“", "", a)
    a = re.sub("”", "", a)
    a = a.strip()
    a = re.sub(r'\x20+','_', a)
    return a


def preprocess_dataset(req):
    
    preprocess_req = re.sub("\\x3A", "", req)
    preprocess_req = re.sub(r'((“|”).*?(”|“))',replaceString,preprocess_req)
    preprocess_req = re.sub("\(|\)", "", preprocess_req)
    preprocess_req = re.sub("\\x27", "", preprocess_req)
    preprocess_req = re.sub("\x2D", "\x5F", preprocess_req)
    preprocess_req = re.sub(r'\x2c{2,4}',r'\x2c',preprocess_req)
    preprocess_req = re.sub(r'\x3B','',preprocess_req)
    preprocess_req = re.sub(r'<lb>(Note|Notes|NOTE).+([a-zA-Z]|[0-9]|\.)$','',preprocess_req)
    preprocess_req = re.sub(r'<lb>(To illustrate).+\.','',preprocess_req)
    preprocess_req = re.sub(r'(<lb><bullet>)(.*)(,|;)',r'\1\2',preprocess_req)
    preprocess_req = re.sub(r'<lb><bullet>','\*',preprocess_req)
    preprocess_req = re.sub(r'<lb>','\x2C\x20',preprocess_req)
    preprocess_req = re.sub(r'(\w+)(\x20)(\x2C)',r'\1\3',preprocess_req)
    preprocess_req = re.sub(r'\x2c\x20*\x2c','\x2c',preprocess_req)
    preprocess_req = re.sub(r'(lock\w+)\x20{1,3}(in)',r'\1_\2',preprocess_req)
    preprocess_req = re.sub(r'\x20{2,}','\x20',preprocess_req)
    return preprocess_req


def tokenize_requirement(requirement, model_tokenizer):
    doc = model_tokenizer(requirement)
    tokenized_requirement = ""
    for token in doc:
        tokenized_requirement += token.text+" "
    tokenized_requirement = tokenized_requirement.strip()
    return tokenized_requirement



def add_reqs_parsing_tree(filename, predictor, model_tokenizer):
    with open(filename) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=';')
        reqs_parsing_trees = []
        for row in csv_reader:
            # I need to change so that the "0"-> ID and "1" -> Requirement
            req_id = row[0]
            req = row[1]  #verify in the dataset in which field it is stored the requirement
            row_with_parsing_tree = []
            if req is not None:
                row_with_parsing_tree.append(req_id)
                row_with_parsing_tree.append(req)
                req_preprocessed = preprocess_dataset(req)
                req_tokenized = tokenize_requirement(req_preprocessed, model_tokenizer)
                parsing_req = predictor.predict_json({"sentence":req_preprocessed})
                constituency_tree = parsing_req['trees']
                row_with_parsing_tree.append(constituency_tree)
                row_with_parsing_tree.append(req_tokenized)
                reqs_parsing_trees.append(row_with_parsing_tree)
    print("Number of requirements on your file: ",len(reqs_parsing_trees))
    return list(reqs_parsing_trees)


def createCsv(content, fileName, outh_path, list_of_list):
    with open(outh_path+'/'+fileName+'.csv', 'w') as csvFile:
        writer = csv.writer(csvFile, delimiter=";", quoting=csv.QUOTE_ALL)
        for row in content:
            if list_of_list:
                writer.writerow(row)
            else:
                writer.writerow([row])
    print("Your files containing the parsing trees have been created.")
    csvFile.close()



def get_parsing_trees_requirements(path, outpath):
    files = []
    for r, d, f in os.walk(path):
        for file in f:
            if '.csv' in file:
                files.append(os.path.join(r, file))

    predictor = Predictor.from_path("https://storage.googleapis.com/allennlp-public-models/elmo-constituency-parser-2020.02.10.tar.gz")
    model_tokenizer = spacy.load("en_core_web_sm")


    for f in files:
        elements = len(f.split("/"))
        file_name = f.split("/")[elements-1]
        file_name = file_name.replace("_tree.csv", "")
        print(file_name)
        reqs_with_parsing_trees = add_reqs_parsing_tree(f, predictor, model_tokenizer)
        createCsv(reqs_with_parsing_trees, file_name, outpath, True)


def main():
    #print(sys.argv)
    if len(sys.argv) == 3 and sys.argv[1] is not None and sys.argv[2] is not None:
        get_parsing_trees_requirements(sys.argv[1], sys.argv[2])
    else:
        print("Please insert the input and output paths.")


if __name__ == "__main__":
    print("Starting generating the parsing trees for your input.")
    main()
    
#print("Starting extracting the requirements to convert into parsing trees")




