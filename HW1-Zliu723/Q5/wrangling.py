""" wrangling.py - utilities to supply data to the templates.

This file contains a pair of functions for retrieving and manipulating data
that will be supplied to the template for generating the table."""
import csv

def username():
    return 'Zliu723'

def data_wrangling():
    with open('data/movies.csv', 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        table = list()
        # Feel free to add any additional variables
        ...
        
        # Read in the header
        for header in reader:
            break
        
        # Read in each row
        for row in reader:
            table.append(row)
            
            # Only read first 100 data rows - [2 points] Q5.a
            q5_a=table[0:100]

        
        # Order table by the last column - [3 points] Q5.b
        q5_b = q5_a.copy()

        for i in range(len(q5_b)):
            q5_b[i][2] = float(q5_b[i][2])

        q5_b.sort(key= lambda x:x[2], reverse= True)

        #method 2: q5_b.sort(key= lambda x:float(x[2]), reverse= True)
    
    return header, q5_b

