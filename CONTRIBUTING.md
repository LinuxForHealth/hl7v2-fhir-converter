## Contributing In General
Our project gladly welcomes external contributions!  We use [Pull Requests](https://github.com/LinuxForHealth/hl7v2-fhir-converter/pulls).

A good way to familiarize yourself with the codebase and contribution process is
to look at the list of [supported messages and segments](README.md) and choose a segment that is not yet done.  You can also look at our [issue tracker](https://github.com/LinuxForHealth/hl7v2-fhir-converter).
Before embarking on a more ambitious contribution, please [get in touch](#communication) with us.

### Proposing new features

If you would like to implement a new feature, please [raise an issue](https://github.com/LinuxForHealth/hl7v2-fhir-converter/issues)
before sending a pull request so the feature can be discussed. This is to avoid
you wasting your valuable time working on a feature that the project developers
are not interested in accepting into the code base.

### Fixing bugs 

If you would like to fix a bug, please [raise an issue](https://github.com/LinuxForHealth/hl7v2-fhir-converter/issues) before sending a
pull request so it can be tracked.

### Merge approval

A pull request requires approval from at least one of the [maintainers](MAINTAINERS.md).

## Legal

Each source file must include a license header for the Apache
Software License 2.0. Using the SPDX format is the simplest approach.
e.g.

```
/*
 * (C) Copyright <holder> <year of first update>[, <year of last update>]
 *
 * SPDX-License-Identifier: Apache-2.0
 */
```

We have tried to make it as easy as possible to make contributions. This
applies to how we handle the legal aspects of contribution. We use the [Developer's Certificate of Origin 1.1 (DCO)](https://github.com/hyperledger/fabric/blob/master/docs/source/DCO1.1.txt) - which is the same that the Linux® Kernel [community](https://elinux.org/Developer_Certificate_Of_Origin)
uses to manage code contributions.

When submitting a patch for review, we require that you include a sign-off statement in the commit message.

Here is an example Signed-off-by line, which indicates that the
submitter accepts the DCO:

```
Signed-off-by: John Doe <john.doe@example.com>
```

You can include this automatically when you commit a change to your
local git repository using the following command:

```
git commit -s
```

## Communication
To connect with us, please open an [issue](https://github.com/LinuxForHealth/hl7v2-fhir-converter/issues) or contact one of the maintainers via email. 
See the [MAINTAINERS.md](MAINTAINERS.md) page.

## Testing
To ensure a working build, please run the full build from the root of the project before submitting your pull request.
Pull Requests should include necessary updates to unit tests (src/test/java of the corresponding project)

