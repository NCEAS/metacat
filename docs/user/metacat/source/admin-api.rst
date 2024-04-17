Admin API
=========
As a Metacat administrator/operator, you have full privileges to access and contribute data to your
Metacat instance via the `DataONE REST API`_. Additionally, there exist two admin specific endpoints
that you have access to:

.. _DataONE REST API: https://knb.ecoinformatics.org/api

Metacat Administrator REST API
..............................

1. ``PUT /index``

   - **Re-index all objects or index a single pid:**

       PUT /index/{pid1}``

       PUT /index?[all=true] | [&pid={pid}]``

   ::

     # Examples

     PUT /index?all=true // Re-index everything
     PUT /index?pid={pid1}&pid={pid2} // Re-index pid1 and pid2

2. ``PUT /identifiers``

   - **Update the metadata for objects identified by their identifiers:**

       PUT /identifiers/{pid1}``

       PUT /identifiers[/]?[all=true] | [&pid={pid1}] | [&formatId={formatId1}]``

   ::

     # Examples

     PUT /identifiers?{pid1}     // Update pid1
     PUT /identifiers?all=true  // Update everything
     PUT /identifiers?pid={pid1}&pid={pid2}  // Update pid1 and pid2
     PUT /identifiers?formatId={formatId1}&formatId={formatId2} // Update dois with formatId1 or formatId2

     PUT /identifiers?formatId={formatId1}&pid={pid2} // Process all elements in a mixed list