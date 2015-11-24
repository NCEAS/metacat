/**
 * Use parts of this script to judiciously remove/update denyFirst access rules before upgrading to Metacat 2.0.0
 * It is important to examine the access blocks that use denyFirst to be sure that you do not end up granting access to 
 * members of groups who should not have access to objects that their group might have access to.
 * The default behavior for Metacat is to deny public access when it is not explicitly listed as allowed, therefore "deny public" rules are
 * superfluous.
 */

-- Analyze the number of rules that need to be addressed:
select principal_name, perm_type, count(*) 
from xml_access 
where perm_order = 'denyFirst' 
and perm_type = 'deny' 
and principal_name != 'public' 
group by principal_name, perm_type;

-- Look at the complete set of records for anything that might need special attention
-- Pay special attention to group names where it makes the most sense to use a denyFirst policy
select * from xml_access 
where docid in (select docid from xml_access where perm_order = 'denyFirst' and perm_type = 'deny' and principal_name != 'public')
order by docid, principal_name, permission;

-- Then do these steps to update rules to use allowFirst only
-- 1a.) Look at the unnecessary public deny rules:
select count(*) 
from xml_access 
where perm_order = 'denyFirst' 
and perm_type = 'deny' 
and principal_name = 'public';
-- 1b.) Delete the unnecessary public deny rules (this is implicit behavior):
delete from xml_access 
where perm_order = 'denyFirst' 
and perm_type = 'deny' 
and principal_name = 'public';

-- 2a.) Examine the non-public deny rules for anything special:
select * 
from xml_access 
where perm_order = 'denyFirst' 
and perm_type = 'deny' 
and principal_name != 'public';
-- 2b.) Delete the non-public deny rules (after examining them!):
delete from xml_access 
where perm_order = 'denyFirst' 
and perm_type = 'deny' 
and principal_name != 'public';

-- 3a.) Summary of denyFirst rules
select perm_type, count(*) 
from xml_access 
where perm_order = 'denyFirst' 
group by perm_type;
-- 3b.) Update all denyFirst rules to be allowFirst
update xml_access 
set perm_order = 'allowFirst' 
where perm_order = 'denyFirst';
