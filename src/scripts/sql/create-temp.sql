CREATE TABLE temp (
   docid  VARCHAR(250),  -- the local document id #
   rev    INT8,          -- the revision part of the local identifier
   checksum text        -- the checksum of the docid
);