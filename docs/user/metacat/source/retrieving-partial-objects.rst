Retrieving Partial Objects
==========================

Users can retrieve a partial object using the HTTP `Range` header with the DataONE `GET /object/{id}` API.

 - **Syntax:**

  ``-H "Range: bytes={start}-{end}" GET /object/{id}``

 - **Rules:**

  - The ``start`` value **must be specified**, and must be **greater than or equal** to ``0`` (since ``0`` denotes the first byte).
  - ``start`` must be **less than or equal to** ``end``.
  - ``end`` must be **less than the total object size**.
  - If ``end`` is omitted, the response will include all bytes from ``start`` to the end of the object.

::

   # Examples

   // Get the first byte of object `foo.1`
   curl -H "Range: bytes=0-0" -X GET https://knb.ecoinformatics.org/knb/d1/mn/v2/object/foo.1

   // Get 3 bytes of object `foo.1`, starting from the second byte
   curl -H "Range: bytes=1-3" -X GET https://knb.ecoinformatics.org/knb/d1/mn/v2/object/foo.1

   // Get all bytes of object `foo.1` except the first byte
   curl -H "Range: bytes=1-" -X GET https://knb.ecoinformatics.org/knb/d1/mn/v2/object/foo.1

