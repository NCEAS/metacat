ALTER TABLE xml_accesssubtree ADD  CONSTRAINT xml_accesssubtree_docid_fk
                FOREIGN KEY (docid) REFERENCES xml_documents;
