# Publishing PikaORM to Maven Central

Same setup as notch. See `notch/dev/PUBLISHING.md` for full details on the
account, GPG key, portal token, and the `maven_keys` secrets repo.

## Namespace

Publishing uses groupId `edu.montana.cs.pika`. The account has the
`edu.montana` namespace verified on central.sonatype.com, which covers all
sub-namespaces, so no additional namespace setup is needed.

The GPG key and portal token in `maven_keys` are account-wide and work for
this namespace too; nothing new to generate.

## How to publish

Make sure the private secrets repo is cloned (gitignored) into `publishing/`:

```sh
git clone git@github.com:msu/maven_keys.git publishing/maven_keys
```

For each release:

```sh
# 1. Set a release version (no -SNAPSHOT) in pom.xml, then build, sign, and stage.
mvn -s publishing/maven_keys/settings.xml clean deploy -Pcentral

# 2. Review the staged deployment on the Portal and click Publish (autoPublish is false).

# 3. Tag the release.
git tag v0.1.0
git push origin v0.1.0
```

Artifact should appear at
`https://central.sonatype.com/artifact/edu.montana.cs.pika/pika-orm`
after a few minutes.

## Notes

- The `central` profile in `pom.xml` mirrors notch's: source + javadoc jars,
  GPG key import at validate, signing at verify, and the
  `central-publishing-maven-plugin` upload at deploy.
- Unlike notch there is no shaded uber-jar, so no shade-skip property.
- Central rejects `-SNAPSHOT` versions; always release from a plain version.