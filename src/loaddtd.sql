DELETE FROM xml_catalog 
      WHERE entry_type LIKE 'DTD'
        AND public_id LIKE '%@eml-version@%';
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-access-@eml-version@//EN',
         '/dtd/eml-access-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-attribute-@eml-version@//EN',
          '/dtd/eml-attribute-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-constraint-@eml-version@//EN',
          '/dtd/eml-constraint-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-coverage-@eml-version@//EN',
          '/dtd/eml-coverage-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-dataset-@eml-version@//EN',
          '/dtd/eml-dataset-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-entity-@eml-version@//EN',
          '/dtd/eml-entity-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-literature-@eml-version@//EN',
          '/dtd/eml-literature-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-physical-@eml-version@//EN',
          '/dtd/eml-physical-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-project-@eml-version@//EN',
          '/dtd/eml-project-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-protocol-@eml-version@//EN',
          '/dtd/eml-protocol-@eml-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-software-@eml-version@//EN',
          '/dtd/eml-software-@eml-version@.dtd');
