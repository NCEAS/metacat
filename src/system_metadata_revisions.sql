/**
 * Correct generated System Metadata entries
 * 1. find system metadata with incomplete revision history
 */

-- these are old (obsoleted) entries that are not marked as such
select sm.guid, sm.obsoleted_by, sm.obsoletes, sm_by.guid as should_be_obsoleted_by
from systemmetadata sm, systemmetadata sm_by
where sm.guid = sm_by.obsoletes
and sm.obsoleted_by is null;
-- update them
BEGIN;
update systemmetadata sm
set obsoleted_by = sm_by.guid,
date_modified = now()
from systemmetadata sm_by
where sm.guid = sm_by.obsoletes
and sm.obsoleted_by is null;
--ROLLBACK;
COMMIT;

-- these are ones that should be marked as newer revisions
select sm.guid, sm.obsoleted_by, sm.obsoletes, sm_s.guid as should_obsolete
from systemmetadata sm, systemmetadata sm_s
where sm.guid = sm_s.obsoleted_by
and sm.obsoletes is null;

-- these are ones that should be marked as archived=true but are not
select sm. guid --count(sm.guid)
from systemmetadata sm, identifier id
where sm.guid = id.guid
and not exists (select * from xml_documents doc where doc.docid = id.docid and doc.rev = id.rev)
and sm.archived != true
and sm.obsoleted_by is null;

-- update them
BEGIN;
update systemmetadata sm
set archived = true,
date_modified = now()
from identifier id
where sm.guid = id.guid
and not exists (select * from xml_documents doc where doc.docid = id.docid and doc.rev = id.rev)
and sm.archived != true
and sm.obsoleted_by is null;
COMMIT;
--ROLLBACK;