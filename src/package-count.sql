column doctype format a50;
SELECT doctype, count(*) cnt 
  FROM xml_documents 
 WHERE doctype IN (
           'eml://ecoinformatics.org/eml-2.0.1',
           '-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN',
           '-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN',
           'eml://ecoinformatics.org/eml-2.0.0'
       )
 GROUP BY doctype 
 ORDER BY cnt DESC
/
