-- use default values initially
CREATE TABLE doi_registration AS

select 
text('testing') as ezid_account,
doc.doctype,
id.guid, 
'https://cn.dataone.org/cn/v1/resolve/' || regexp_replace(regexp_replace(id.guid, '/', '%2F', 'g'), ':', '%3A', 'g') as url, 
text('Unknown') as title, 
text(null) as creator,
text(null) as publisher,
to_char(doc.date_created, 'YYYY') as pub_date
from identifier id, xml_documents doc
where guid like 'doi%'
and id.docid = doc.docid
and id.rev = doc.rev

UNION ALL

select 
text('testing') as ezid_account,
doc.doctype,
id.guid, 
'https://cn.dataone.org/cn/v1/resolve/' || regexp_replace(regexp_replace(id.guid, '/', '%2F', 'g'), ':', '%3A', 'g') as url, 
text('Unknown') as title, 
text(null) as creator,
text(null) as publisher,
to_char(doc.date_created, 'YYYY') as pub_date
from identifier id, xml_revisions doc
where guid like 'doi%'
and id.docid = doc.docid
and id.rev = doc.rev;

--update defaults
update doi_registration
set title = 'Data file'
where doctype = 'BIN';

update doi_registration
set title = 'Legacy EML file'
where doctype like '%eml%2.0.0beta%';

-- update title using node information
update doi_registration doi
set title = child.nodedata
from identifier id, xml_documents docs, xml_nodes nodes, xml_nodes parent, xml_nodes child
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and nodes.nodename = 'title'
and parent.nodename = 'dataset';

-- update pubDate using nodes
update doi_registration doi
set pub_date = child.nodedata
from identifier id, xml_documents docs, xml_nodes nodes, xml_nodes parent, xml_nodes child
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and nodes.nodename = 'pubDate'
and parent.nodename = 'dataset';

-- update publisher using nodes
update doi_registration doi
set publisher = child.nodedata
from identifier id, xml_documents docs, xml_nodes nodes, xml_nodes parent, xml_nodes child
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and nodes.nodename = 'organizationName'
and parent.nodename = 'publisher'
and nodes.nodeid = (select min(thisnode.nodeid) from xml_nodes thisnode where thisnode.parentnodeid = parent.nodeid and nodetype = 'ELEMENT');

-- update publisher surName
update doi_registration doi
set publisher = child.nodedata
from identifier id, 
xml_documents docs, 
xml_nodes nodes, 
xml_nodes parent, 
xml_nodes child, 
xml_nodes grandparent
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and parent.parentnodeid = grandparent.nodeid
and nodes.nodename = 'surName'
and parent.nodename = 'individualName'
and grandparent.nodename = 'publisher'
and parent.nodeid = (select min(thisnode.nodeid) from xml_nodes thisnode where thisnode.parentnodeid = grandparent.nodeid and nodetype = 'ELEMENT');

-- add the first name if we have it
update doi_registration doi
set publisher = publisher ||', '|| child.nodedata
from identifier id, 
xml_documents docs, 
xml_nodes nodes, 
xml_nodes parent, 
xml_nodes child, 
xml_nodes grandparent
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and parent.parentnodeid = grandparent.nodeid
and nodes.nodename = 'givenName'
and parent.nodename = 'individualName'
and grandparent.nodename = 'publisher'
and parent.nodeid = (select min(thisnode.nodeid) from xml_nodes thisnode where thisnode.parentnodeid = grandparent.nodeid and nodetype = 'ELEMENT');

-- update organization using nodes
update doi_registration doi
set creator = child.nodedata
from identifier id, xml_documents docs, xml_nodes nodes, xml_nodes parent, xml_nodes child
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and nodes.nodename = 'organizationName'
and parent.nodename = 'creator'
and nodes.nodeid = (select min(thisnode.nodeid) from xml_nodes thisnode where thisnode.parentnodeid = parent.nodeid and nodetype = 'ELEMENT');

-- update creator using nodes
-- creator/individualName/surName
-- creator/individualName/givenName
update doi_registration doi
set creator = child.nodedata
from identifier id, 
xml_documents docs, 
xml_nodes nodes, 
xml_nodes parent, 
xml_nodes child, 
xml_nodes grandparent
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and parent.parentnodeid = grandparent.nodeid
and nodes.nodename = 'surName'
and parent.nodename = 'individualName'
and grandparent.nodename = 'creator'
and parent.nodeid = (select min(thisnode.nodeid) from xml_nodes thisnode where thisnode.parentnodeid = grandparent.nodeid and nodetype = 'ELEMENT');

-- add the first name if we have it
update doi_registration doi
set creator = creator ||', '|| child.nodedata
from identifier id, 
xml_documents docs, 
xml_nodes nodes, 
xml_nodes parent, 
xml_nodes child, 
xml_nodes grandparent
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and parent.parentnodeid = grandparent.nodeid
and nodes.nodename = 'givenName'
and parent.nodename = 'individualName'
and grandparent.nodename = 'creator'
and parent.nodeid = (select min(thisnode.nodeid) from xml_nodes thisnode where thisnode.parentnodeid = grandparent.nodeid and nodetype = 'ELEMENT');

-- update title using revisions
update doi_registration doi
set title = child.nodedata
from identifier id, xml_revisions docs, xml_nodes_revisions nodes, xml_nodes_revisions parent, xml_nodes_revisions child
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and nodes.nodename = 'title'
and parent.nodename = 'dataset';

-- update pubDate using revisions
update doi_registration doi
set pub_date = child.nodedata
from identifier id, xml_revisions docs, xml_nodes_revisions nodes, xml_nodes_revisions parent, xml_nodes_revisions child
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and nodes.nodename = 'pubDate'
and parent.nodename = 'dataset';

-- update publisher using revisions
update doi_registration doi
set publisher = child.nodedata
from identifier id, xml_revisions docs, xml_nodes_revisions nodes, xml_nodes_revisions parent, xml_nodes_revisions child
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and nodes.nodename = 'organizationName'
and parent.nodename = 'publisher'
and nodes.nodeid = (select min(thisnode.nodeid) from xml_nodes_revisions thisnode where thisnode.parentnodeid = parent.nodeid and nodetype = 'ELEMENT');

-- use publisher individual name if we have it
update doi_registration doi
set publisher = child.nodedata
from identifier id, 
xml_revisions docs, 
xml_nodes_revisions nodes, 
xml_nodes_revisions parent, 
xml_nodes_revisions child, 
xml_nodes_revisions grandparent
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and parent.parentnodeid = grandparent.nodeid
and nodes.nodename = 'surName'
and parent.nodename = 'individualName'
and grandparent.nodename = 'publisher'
and parent.nodeid = (select min(thisnode.nodeid) from xml_nodes_revisions thisnode where thisnode.parentnodeid = grandparent.nodeid and nodetype = 'ELEMENT');

--include the first name (careful not to rerun this part)
update doi_registration doi
set publisher = publisher ||', '|| child.nodedata
from identifier id, 
xml_revisions docs, 
xml_nodes_revisions nodes, 
xml_nodes_revisions parent, 
xml_nodes_revisions child, 
xml_nodes_revisions grandparent
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and parent.parentnodeid = grandparent.nodeid
and nodes.nodename = 'givenName'
and parent.nodename = 'individualName'
and grandparent.nodename = 'publisher'
and parent.nodeid = (select min(thisnode.nodeid) from xml_nodes_revisions thisnode where thisnode.parentnodeid = grandparent.nodeid and nodetype = 'ELEMENT');

-- update organization using nodes
update doi_registration doi
set creator = child.nodedata
from identifier id, xml_revisions docs, xml_nodes_revisions nodes, xml_nodes_revisions parent, xml_nodes_revisions child
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and nodes.nodename = 'organizationName'
and parent.nodename = 'creator'
and nodes.nodeid = (select min(thisnode.nodeid) from xml_nodes_revisions thisnode where thisnode.parentnodeid = parent.nodeid and nodetype = 'ELEMENT');

-- update creator using revisions
update doi_registration doi
set creator = child.nodedata
from identifier id, 
xml_revisions docs, 
xml_nodes_revisions nodes, 
xml_nodes_revisions parent, 
xml_nodes_revisions child, 
xml_nodes_revisions grandparent
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and parent.parentnodeid = grandparent.nodeid
and nodes.nodename = 'surName'
and parent.nodename = 'individualName'
and grandparent.nodename = 'creator'
and parent.nodeid = (select min(thisnode.nodeid) from xml_nodes_revisions thisnode where thisnode.parentnodeid = grandparent.nodeid and nodetype = 'ELEMENT');

-- add the first name if we have it
update doi_registration doi
set creator = creator ||', '|| child.nodedata
from identifier id, 
xml_revisions docs, 
xml_nodes_revisions nodes, 
xml_nodes_revisions parent, 
xml_nodes_revisions child, 
xml_nodes_revisions grandparent
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and parent.parentnodeid = grandparent.nodeid
and nodes.nodename = 'givenName'
and parent.nodename = 'individualName'
and grandparent.nodename = 'creator'
and parent.nodeid = (select min(thisnode.nodeid) from xml_nodes_revisions thisnode where thisnode.parentnodeid = grandparent.nodeid and nodetype = 'ELEMENT');

-- use xml_documents for defaults that are still missing 
update doi_registration doi
set creator = doc.user_owner
from xml_documents doc, identifier id
where doi.guid = id.guid
and id.docid = doc.docid
and id.rev = doc.rev
and doi.creator is null;

update doi_registration doi
set creator = doc.user_owner
from xml_revisions doc, identifier id
where doi.guid = id.guid
and id.docid = doc.docid
and id.rev = doc.rev
and doi.creator is null;

-- set publisher
update doi_registration
set publisher = creator
where publisher is null or publisher = '';

-- clean up
update doi_registration
set title = replace(title, E'\n', ' ');

update doi_registration
set title = regexp_replace(title, E'\\s+', ' ', 'g');

update doi_registration
set title = regexp_replace(title, E'^\\s+', '', 'g');

update doi_registration
set ezid_account = 'sb_nceas'
where guid like 'doi:10.5063%';
--16988

update doi_registration
set ezid_account = 'sb_pisco'
where guid like 'doi:10.6085%';
--98078

update doi_registration
set ezid_account = 'lternet'
where guid like 'doi:10.6073%';
--49482

/**
 * Additional modifications, July 5th, 2012
 */
ALTER TABLE doi_registration
ADD COLUMN resourceType text,
ADD COLUMN objectFormat text,
ADD COLUMN obsoletedBy text,
ADD COLUMN obsoletes text,
ADD COLUMN resourceMapId text,
ADD COLUMN resourceMapLocation text,
ADD COLUMN access text;

-- update access values
update doi_registration
set access = 'protected';

update doi_registration doi
set access = 'public'
from xml_access a
where doi.guid = a.guid
and a.principal_name = 'public'
and a.permission >= '4'
and a.perm_type = 'allow';

-- update the objectFormat from xml_documents
update doi_registration doi
set objectFormat = doc.doctype
from identifier id, xml_documents doc
where doi.guid = id.guid
and id.docid = doc.docid
and id.rev = doc.rev;

-- update the objectFormat from xml_revisions
update doi_registration doi
set objectFormat = rev.doctype
from identifier id, xml_revisions rev
where doi.guid = id.guid
and id.docid = rev.docid
and id.rev = rev.rev;

--update resourceType
update doi_registration
set resourceType = 'Dataset/metadata';

update doi_registration
set resourceType = 'Dataset/data'
where objectFormat = 'BIN';

-- update the data file titles
update doi_registration doi
set title = 'Data file - ' || doc.docname
from identifier id, xml_documents doc
where doi.guid = id.guid
and id.docid = doc.docid
and id.rev = doc.rev
and doi.resourceType = 'Dataset/data';

-- update the data file titles from revisions
update doi_registration doi
set title = 'Data file - ' || doc.docname
from identifier id, xml_revisions doc
where doi.guid = id.guid
and id.docid = doc.docid
and id.rev = doc.rev
and doi.resourceType = 'Dataset/data';

-- update the objectFormat from SM table (will be subset)
update doi_registration doi
set objectFormat = sm.object_format
from systemMetadata sm
where doi.guid = sm.guid;
--16938

--update revision history from SM
update doi_registration doi
set obsoletes = sm.obsoletes,
obsoletedBy = sm.obsoleted_by
from systemMetadata sm
where doi.guid = sm.guid;

/** use plain old revision history **/
-- update obsoletedby
update doi_registration doi
set obsoletedBy = newer.guid
from identifier id, identifier newer
where doi.guid = id.guid
and id.docid = newer.docid
and newer.rev = (select min(next.rev) from identifier next where next.docid = id.docid and next.rev > id.rev);

-- update the obsolets
update doi_registration doi
set obsoletes = older.guid
from identifier id, identifier older
where doi.guid = id.guid
and id.docid = older.docid
and older.rev = (select max(prev.rev) from identifier prev where prev.docid = id.docid and prev.rev < id.rev);

/**
select doi.guid, older.guid as obsoletes 
from doi_registration doi, identifier id, identifier older
where doi.guid = id.guid
and id.docid = older.docid
and older.rev = (select max(prev.rev) from identifier prev where prev.docid = id.docid and prev.rev < id.rev)
and doi.guid like 'doi:10.5063/AA/ABS.4%';
**/


--update the resourcemapid for described data
CREATE TABLE ecogrid_docids AS
select distinct id.docid, id.rev, id.guid, substring(xpi.nodedata from 'ecogrid://knb/(.*)$') as data_docid
from xml_path_index xpi, identifier id, xml_documents xmld
where xpi.docid = xmld.docid 
and xmld.docid = id.docid
and xmld.rev = id.rev
and xpi.path like 'dataset/%/physical/distribution/online/url' 
and xpi.nodedata like 'ecogrid%'
and id.guid in (select guid from doi_registration);

-- include revisions for described data
INSERT INTO ecogrid_docids
select distinct id.docid, id.rev, id.guid, substring(child.nodedata from 'ecogrid://knb/(.*)$') as data_docid
from identifier id, xml_revisions docs, xml_nodes_revisions nodes, xml_nodes_revisions parent, xml_nodes_revisions child
where id.docid = docs.docid
and id.rev = docs.rev
and docs.rootnodeid = nodes.rootnodeid
and nodes.nodeid = child.parentnodeid
and nodes.parentnodeid = parent.nodeid
and nodes.nodename = 'url'
and parent.nodename = 'online'
and child.nodedata like 'ecogrid%'
and child.nodetype = 'TEXT'
and id.guid in (select guid from doi_registration);

-- Set the resource map for the data files (NOTE: some of thee maps might not actually exist on the system depending on how successful the ORE generation was)
update doi_registration doi
set resourceMapId = 'resourceMap_'||eco.docid||'.'||eco.rev
from identifier id, ecogrid_docids eco
where doi.guid = id.guid
and id.docid||'.'||id.rev = eco.data_docid;

-- set the resource map id for the metadata file that did most of the packaging work!
update doi_registration doi
set resourceMapId = 'resourceMap_'||eco.docid||'.'||eco.rev
from ecogrid_docids eco
where doi.guid = eco.guid;

select count(*)
from doi_registration
where resourceMapId is null;

--update the resource map location
update doi_registration
set resourceMapLocation = 'https://cn.dataone.org/cn/v1/resolve/' || resourceMapId
where resourceMapId is not null;

-- fix null values (should not have to have this now, but it will not hurt)
update doi_registration doi
set creator = user_owner
from identifier id, xml_documents docs
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev= docs.rev
and (creator is null or trim(creator) = '');

update doi_registration doi
set publisher = user_owner
from identifier id, xml_documents docs
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev= docs.rev
and (publisher is null or trim(publisher) = '');

update doi_registration doi
set creator = user_owner
from identifier id, xml_revisions docs
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev= docs.rev
and (creator is null or trim(creator) = '');

update doi_registration doi
set publisher = user_owner
from identifier id, xml_revisions docs
where doi.guid = id.guid
and id.docid = docs.docid
and id.rev= docs.rev
and (publisher is null or trim(publisher) = '');

-- fix a previous mistake
update doi_registration
set publisher = null
where publisher = 'document';

--update creator usinig LDAP lookup
CREATE TABLE ecoinfo_dn (dn text, givenName text, sn text);
--TRUNCATE TABLE ecoinfo_dn;
COPY ecoinfo_dn FROM '/tmp/ecoinfo_dn.csv' WITH CSV HEADER;

update doi_registration
set creator = 
case when givenName is null then
	sn
else 
	sn || ', ' || givenName
end
from ecoinfo_dn
where trim(both from lower(creator)) = lower(dn);

update doi_registration
set publisher = 
case when givenName is null then
	sn
else 
	sn || ', ' || givenName
end
from ecoinfo_dn
where trim(both from lower(publisher)) = lower(dn);

select creator, count(*) as cnt
from doi_registration
where creator like 'uid=%'
group by creator
order by cnt desc;

-- update the data files with owner/publisher from EML entry
update doi_registration doi
set creator = meta.creator,
publisher = meta.publisher
from doi_registration meta, identifier id, ecogrid_docids eco
where doi.guid = id.guid
and id.docid||'.'||id.rev = eco.data_docid
and eco.guid = meta.guid;

-- clean up KBS entries that have empty user_owner
update doi_registration
set creator = 'Kellogg Biological Station'
where (creator is null or trim(creator) = '')
and guid like '%knb-lter-kbs%';

update doi_registration
set publisher = 'Kellogg Biological Station'
where (publisher is null or trim(publisher) = '')
and guid like '%knb-lter-kbs%';

-- more clean up
update doi_registration
set creator = trim(creator),
publisher = trim(publisher);

-- set the publisher to the source system if it is the same as the creator
update doi_registration
set publisher = 
case 
when guid like 'doi:10.5063%' then
	'Knowledge Network for Biocomplexity (KNB)'
when guid like 'doi:10.6085%' then
	'Partnership for Interdisciplinary Studies of Coastal Oceans (PISCO)'
when guid like 'doi:10.6073%' then
	'Long Term Ecological Research Network (LTER)'
else
	publisher
end
where publisher = creator;

-- entity references
update doi_registration
set publisher = regexp_replace(publisher, '&amp;', '&', 'g');
update doi_registration
set publisher = regexp_replace(publisher, '&apos;', E'\'', 'g');
--accented i (í)
update doi_registration
set publisher = regexp_replace(publisher, '&#237;', 'í', 'g')
where publisher like '%&#237;%';
update doi_registration
set creator = regexp_replace(creator, '&amp;', '&', 'g');
update doi_registration
set creator = regexp_replace(creator, '&apos;', E'\'', 'g');

--titles
update doi_registration
set title = regexp_replace(title, '&amp;', '&', 'g');
update doi_registration
set title = regexp_replace(title, '&mp;', '&', 'g')
where title like '%&mp;%';
update doi_registration
set title = regexp_replace(title, '&apos;', E'\'', 'g');
update doi_registration
set title = regexp_replace(title, '&quot;', E'\'', 'g');
update doi_registration
set title = regexp_replace(title, '&gt;', '>', 'g');
update doi_registration
set title = regexp_replace(title, '&lt;', '<', 'g');
update doi_registration
set title = regexp_replace(title, '&#237;', 'í', 'g')
where title like '%&#237;%';
update doi_registration
set title = regexp_replace(title, '&#195;&#173;', 'í', 'g')
where title like '%&#195;&#173;%';
update doi_registration
set title = regexp_replace(title, '&#195;&#177;', 'í', 'g')
where title like '%&#195;&#177;%';
update doi_registration
set title = regexp_replace(title, '&#243;', 'ó', 'g')
where title like '%&#243;%';
update doi_registration
set title = regexp_replace(title, '&#227;', 'ã', 'g')
where title like '%&#227;%';
update doi_registration
set title = regexp_replace(title, '&#231;', 'ç', 'g')
where title like '%&#231;%';
update doi_registration
set title = regexp_replace(title, '&#233;', 'é', 'g')
where title like '%&#233;%';
update doi_registration
set title = regexp_replace(title, '&#241;', 'ñ', 'g')
where title like '%&#241;%';
update doi_registration
set title = regexp_replace(title, '&#226;&#128;&#147;', '-', 'g')
where title like '%&#226;&#128;&#147;%';
update doi_registration
set title = regexp_replace(title, '&#226;&#128;&#153;', E'\'', 'g')
where title like '%&#226;&#128;&#153;%';
update doi_registration
set title = regexp_replace(title, '&#8211;', '/', 'g')
where title like '%&#8211;%';
update doi_registration
set title = regexp_replace(title, '&#195;&#161;', 'á', 'g')
where title like '%&#195;&#161;%';
update doi_registration
set title = regexp_replace(title, '&#195;&#179;', 'ó', 'g')
where title like '%&#195;&#179;%';
--&#26684;&#24335;&#31684;&#20363; (格式范例)
update doi_registration
set title = regexp_replace(title, '&#26684;&#24335;&#31684;&#20363;', '格式范例', 'g')
where title like '%&#26684;&#24335;&#31684;&#20363;%';

--directional quotes
update doi_registration
set title = regexp_replace(title, '&#226;&#128;&#157;', '"', 'g')
where title like '%&#226;&#128;&#157;%';
update doi_registration
set title = regexp_replace(title, '&#226;&#128;&#156;', '"', 'g')
where title like '%&#226;&#128;&#156;%';
--one off apostrophe
update doi_registration
set title = regexp_replace(title, '&#213;', E'\'', 'g')
where title like '%&#213;%';
update doi_registration
set title = regexp_replace(title, '&#150;', '-', 'g')
where title like '%&#150;%';

--after reading the complete metadata, I could interpret these as "greater than"
update doi_registration
set title = regexp_replace(title, '&t;', '>', 'g')
where title like '%&t;%'
and guid in ('doi:10.6073/AA/knb-lter-luq.15.1', 'doi:10.6073/AA/knb-lter-luq.39.1', 'doi:10.6073/AA/knb-lter-luq.37.1', 'doi:10.6073/AA/knb-lter-luq.40.1', 'doi:10.6073/AA/knb-lter-luq.18.1' );


--special characters
update doi_registration
set creator = regexp_replace(creator, '&#233;', 'é', 'g')
where creator like '%&#233;%';
update doi_registration
set creator = regexp_replace(creator, '&#231;', 'ç', 'g')
where creator like '%&#231;%';
update doi_registration
set creator = regexp_replace(creator, '&#241;', 'ñ', 'g')
where creator like '%&#241;%';
-- Guti&#195;&#169;rrez, Ralph, J.
-- Li&#195;&#169;bault,  Fr&#195;&#169;d&#195;&#169;ric
update doi_registration
set creator = regexp_replace(creator, '&#195;&#169;', 'é', 'g')
where creator like '%&#195;&#169;%';
-- Ram&#195;&#173;rez, Alonso
update doi_registration
set creator = regexp_replace(creator, '&#195;&#173;', 'í', 'g')
where creator like '%&#195;&#173;%';
-- Gonz&#195;&#161;lez, Grizelle
update doi_registration
set creator = regexp_replace(creator, '&#195;&#161;', 'á', 'g')
where creator like '%&#195;&#161;%';
--&#29579;,  (王, 名, 姓)
update doi_registration
set creator = regexp_replace(creator, '&#29579;', '王', 'g')
where creator like '%&#29579;%';
--&#21517;
update doi_registration
set creator = regexp_replace(creator, '&#21517;', '名', 'g')
where creator like '%&#21517;%';
--&#22995;
update doi_registration
set creator = regexp_replace(creator, '&#22995;', '姓', 'g')
where creator like '%&#22995;%';
-- These are left
-- Helmbrecht, S &#226;&#128;&#153;rai
-- lk&#195;&#177;lkl, Fgfggf

-- clean up
update doi_registration
set publisher = replace(publisher, E'\n', ' ');
update doi_registration
set publisher = regexp_replace(publisher, E'\\s+', ' ', 'g');
update doi_registration
set publisher = regexp_replace(publisher, E'^\\s+', '', 'g');

-- use only year for pub_date
update doi_registration
set pub_date = to_char(date(pub_date), 'YYYY')
where length(pub_date) > 4
and pub_date like '%-%-%';

--set the rest to upload date, xml_documents
update doi_registration doi
set pub_date = to_char(date_created, 'YYYY')
from xml_documents x, identifier id
where length(pub_date) != 4
and doi.guid = id.guid
and id.docid = x.docid
and id.rev = x.rev;
-- revisions
update doi_registration doi
set pub_date = to_char(date_created, 'YYYY')
from xml_revisions x, identifier id
where length(pub_date) != 4
and doi.guid = id.guid
and id.docid = x.docid
and id.rev = x.rev;

update doi_registration
set creator = replace(creator, E'\n', ' ');

-- some entries got in with blank spaces 
update doi_registration
set title = 'unknown'
where length(trim(title)) = 0;

update doi_registration
set creator = 'unknown'
where length(trim(creator)) = 0;

-- BES has very long publisher fields, so use this default for them all
update doi_registration
set publisher = 'Baltimore Ecosystem Study LTER'
where guid like '%knb-lter-bes%';

-- need URL identifiers for DataCite when they are not DOIs
update doi_registration
set obsoletedBy = 'https://cn.dataone.org/cn/v1/resolve/' || obsoletedBy 
where obsoletedBy not like 'doi%';

update doi_registration
set obsoletes = 'https://cn.dataone.org/cn/v1/resolve/' || obsoletes
where obsoletes not like 'doi%';

ALTER TABLE doi_registration
ADD COLUMN obsoletedByIdType text,
ADD COLUMN obsoletesIdType text,
ADD COLUMN resourceMapLocationIdType text;

update doi_registration
set obsoletedByIdType = 'DOI'
where obsoletedBy is not null
and obsoletedBy like 'doi%';

update doi_registration
set obsoletedByIdType = 'URL'
where obsoletedBy is not null
and obsoletedBy not like 'doi%';

update doi_registration
set obsoletesIdType = 'DOI'
where obsoletes is not null
and obsoletes like 'doi%';

update doi_registration
set obsoletesIdType = 'URL'
where obsoletes is not null
and obsoletes not like 'doi%';

update doi_registration
set resourceMapLocationIdType = 'URL'
where resourceMapLocation is not null;

-- copy to the external file
COPY 
(select ezid_account, 
guid as dc_identifier, 
url as datacite_url, 
title as dc_title, 
creator as dc_creator, 
publisher as dc_publisher, 
pub_date as datacite_publicationYear,
split_part(resourceType, '/', 1) as datacite_resourceTypeGeneral,
split_part(resourceType, '/', 2) as datacite_resourceType,
objectFormat as datacite_format,
obsoletedBy as datacite_relatedIdentifier_isPreviousVersionOf,
obsoletedByIdType as datacite_relatedIdentifier_isPreviousVersionOfType,
obsoletes as datacite_relatedIdentifier_isNewVersionOf,
obsoletesIdType as datacite_relatedIdentifier_isNewVersionOfType,
resourceMapLocation as datacite_relatedIdentifier_isPartOf,
resourceMapLocationIdType as datacite_relatedIdentifier_isPartOfType,
access as d1_read_access
from doi_registration 
where access = 'public'
order by dc_identifier) 
TO '/tmp/doi_registration.csv'
WITH CSV HEADER;
--164548

--drop table doi_registration;
--drop table ecogrid_docids;
--drop table ecoinfo_dn;

