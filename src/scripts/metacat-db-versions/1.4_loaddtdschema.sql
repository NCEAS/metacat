DELETE FROM xml_catalog 
      WHERE entry_type LIKE 'DTD'
        AND public_id LIKE '%2.0.0beta6%';
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-access-2.0.0beta6//EN',
         '/dtd/eml-access-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-attribute-2.0.0beta6//EN',
          '/dtd/eml-attribute-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-constraint-2.0.0beta6//EN',
          '/dtd/eml-constraint-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-coverage-2.0.0beta6//EN',
          '/dtd/eml-coverage-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN',
          '/dtd/eml-dataset-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-entity-2.0.0beta6//EN',
          '/dtd/eml-entity-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-literature-2.0.0beta6//EN',
          '/dtd/eml-literature-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-physical-2.0.0beta6//EN',
          '/dtd/eml-physical-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-project-2.0.0beta6//EN',
          '/dtd/eml-project-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-protocol-2.0.0beta6//EN',
          '/dtd/eml-protocol-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-software-2.0.0beta6//EN',
          '/dtd/eml-software-2.0.0beta6.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'eml://ecoinformatics.org/eml-2.0.1', '/schema/eml-2.0.1/eml.xsd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'eml://ecoinformatics.org/eml-2.0.0', '/schema/eml-2.0.0/eml.xsd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://www.xml-cml.org/schema/stmml', '/schema/eml-2.0.0/stmml.xsd');
