# AWS S3 image uploads

The application uploads images to a private S3 bucket and stores each stable
S3 object URL in the `images.url` column. The API returns a separate pre-signed
URL for viewing; it expires after 10 minutes, so a fresh one is generated for
each response.

## How image data flows through the code

- `ImageController` receives multipart files and delegates to `ImageService`.
- `ImageServiceImpl` validates each supported image or document, uploads it, then saves its
  stable S3 URL and brand relationship through `ImageRepository`.
- `S3ServiceImpl` uploads file bytes and creates temporary read URLs. It does
  not write to the database.
- `Image` is the JPA entity for the `images` table. Its `url` field stores the
  stable S3 object URL.
- `ImageRepository` loads file rows. The brand-file GET endpoint and product
  search convert each stored S3 URL into a fresh signed URL before returning it.

After uploading for brand `1`, the database row is conceptually:

| id | brand_id | url |
|---:|---:|---|
| 1 | 1 | `https://rovique.s3.eu-north-1.amazonaws.com/brands/1/8c...jpg` |

The API response contains a temporary signed URL, not the stored URL. The
stored S3 URL identifies the object but does not make a private object public.

## Create and configure AWS resources

1. The `rovique` bucket is in `eu-north-1`. Keep **Block all public access**
   enabled and leave
   object ownership as **Bucket owner enforced**. The app does not need public
   bucket access or ACLs.
2. Create an IAM identity for the application. For local development, create
   an IAM user with a programmatic access key; for deployment, prefer an
   instance/task role instead of long-lived keys. Attach a policy like the
   following, replacing the bucket name:

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": ["s3:PutObject", "s3:GetObject"],
         "Resource": "arn:aws:s3:::YOUR_BUCKET_NAME/*"
       }
     ]
   }
   ```

3. Configure the application environment. Set the region to the bucket's
   actual region. The AWS SDK uses its standard credential chain, so
   credentials should not be placed in source code or committed to
   `application.properties`. For production, prefer an instance/task role
   with the least privilege needed. If static credentials are unavoidable,
   keep them in a managed secret store and inject them at runtime. If an access
   key has been shared or exposed, deactivate it and use a replacement key.

   PowerShell (current terminal only):

   ```powershell
   $env:AWS_S3_BUCKET = "rovique"
   $env:AWS_REGION = "eu-north-1"
   ```

   For local development, store credentials outside the repository in a named
   AWS CLI profile and let the SDK load them:

   ```powershell
   aws configure --profile rovi-product
   $env:AWS_PROFILE = "rovi-product"
   $env:AWS_S3_BUCKET = "rovique"
   $env:AWS_REGION = "eu-north-1"
   ```

   For deployed environments, use an instance/task role instead of access
   keys. See `../SECRETS.md` for managing application secrets and injecting
   them at runtime. Start the app after setting the environment:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

## API

Upload one or more files for an existing brand:

```bash
curl -X POST "http://localhost:8080/api/v1/product/brands/1/files" \
  -F "files=@front.jpg" \
  -F "files=@instructions.pdf"
```

In Postman, choose **Body → form-data**, add a row with key `file`, change its
type to **File**, and select an image. Repeat the `file` row for multiple
images or documents. The API also accepts `files` as the key. Supported types
are JPEG, PNG, GIF, WEBP, PDF, TXT, CSV, DOC/DOCX, XLS/XLSX, and PPT/PPTX.
Each file is limited to 10 MB, and the total multipart request is limited to
50 MB. The response data contains pre-signed URLs.

Fetch fresh pre-signed URLs for files already saved for a brand:

```bash
curl "http://localhost:8080/api/v1/product/brands/1/files"
```
