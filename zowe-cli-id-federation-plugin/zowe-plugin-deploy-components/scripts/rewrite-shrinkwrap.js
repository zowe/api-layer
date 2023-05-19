const fs = require("fs");
const getPackageInfo = require(__dirname + "/utils").getPackageInfo;

const _path = __dirname + "/../temp/package/npm-shrinkwrap.json";
const data = require(_path);

(async () => {
    const filterPkgs = async (obj, key) => {
        const _obj = {};
        for (const pkg of Object.keys(obj[key])) {
            if (obj[key][pkg].dev) continue;
            if (obj[key][pkg].peer) continue;
            if (obj[key][pkg].extraneous) continue;

            _obj[pkg] = obj[key][pkg];

            // Check if the package didn't resolve to public NPM
            if (_obj[pkg].resolved && !_obj[pkg].resolved.startsWith("https://registry.npmjs.org")) {
                const pkgPos = pkg.lastIndexOf("node_modules") + "node_modules".length + 1;

                // Check (and fail) if the package isn't a scoped package
                if(!pkg.startsWith("@") && pkg[pkgPos] !== "@") {
                    console.error("Problematic package:", pkg);
                    throw "Problematic package:" + pkg;
                }

                _obj[pkg].resolved = await getPackageInfo(pkg.substring(pkg.startsWith("@") ? 0 : pkgPos) + "@" + _obj[pkg].version, "", "dist.tarball");
                _obj[pkg].integrity = await getPackageInfo(pkg.substring(pkg.startsWith("@") ? 0 : pkgPos) + "@" + _obj[pkg].version, "", "dist.integrity");
            }
        }
        obj[key] = _obj;
    }

    await filterPkgs(data, "packages");
    await filterPkgs(data, "dependencies");

    fs.writeFileSync(_path, JSON.stringify(data, null, 2) + "\n" );
})();
