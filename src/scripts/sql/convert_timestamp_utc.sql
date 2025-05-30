DO $$
DECLARE v_cut_off_time timestamp;
DECLARE v_org_time_zone text;
BEGIN 
  v_cut_off_time := '2025-05-24 14:30:00.000';
  v_org_time_zone := 'America/Los_Angeles';
  UPDATE systemmetadata SET date_modified=(date_modified::timestamp at time zone v_org_time_zone at time zone 'UTC') WHERE date_modified::timestamp < v_cut_off_time; 
  UPDATE systemmetadata SET date_uploaded=(date_uploaded::timestamp at time zone v_org_time_zone at time zone 'UTC') WHERE date_uploaded::timestamp < v_cut_off_time;
  UPDATE smreplicationstatus SET date_verified=(date_verified::timestamp at time zone v_org_time_zone at time zone 'UTC') WHERE date_verified::timestamp < v_cut_off_time;
  UPDATE access_log SET date_logged=(date_logged::timestamp at time zone v_org_time_zone at time zone 'UTC') WHERE date_logged::timestamp < v_cut_off_time;
  UPDATE index_event SET event_date=(event_date::timestamp at time zone v_org_time_zone at time zone 'UTC') WHERE event_date::timestamp < v_cut_off_time;
END $$