die () {
    echo "$@"
    exit 1
}

[ "$#" -eq 3 ] || die "3 arguments required, $# provided"

tarfile=$1;
registry=$2;
newversion=$3;

mkdir temp
tar xzf $tarfile -C temp
cd temp
cd package
# Unholy one liner which replace registry and repository with blank strings. Should convert this to javascript file soonTM.
# Also remove prepare script which may require dev dependencies like Husky - https://github.com/typicode/husky/issues/914
# Also remove prepack script which may require scripts from the project repo
# Takes in package.json, outputs package_new.json
node -e "package = require('./package.json');
    package.publishConfig.registry='$registry';
    package.version='$newversion';
    delete package.scripts.prepare;
    delete package.scripts.prepack;
    require('fs').writeFileSync('package_new.json', JSON.stringify(package, null, 2), 'utf8')"
# Move the old package JSON to build dir so we can publish as a Jenkins artifact?
mv package.json ../../$tarfile.json
# Replace package json with our new one
mv package_new.json package.json

# Check that all dependencies are valid
npmDeps=`node -e "package = require('./package.json');
    Object.entries(package.dependencies || {}).forEach(([name, version]) => console.log(name + '@' + version));
    Object.entries(package.peerDependencies || {}).forEach(([name, version]) => console.log(name + '@' + version));"`
for pkgSpec in $npmDeps; do
    echo "Validating dependency $pkgSpec..."
    npm view $pkgSpec || exit 1
done

# Update npm-shrinkwrap.json if necessary
if [ -e "npm-shrinkwrap.json" ]; then
    # Create a production environment (taking in consideration the npm-shrinkwrap)
    npm install --only=prod --ignore-scripts

    # Rewrite the shrinkwrap file with only production dependencies and public npm resolved URLs
    node "../../scripts/rewrite-shrinkwrap.js"
fi

npm pack

# delete the original tar
rm -f ../../$tarfile

#move the new tar into the original directory
mv *.tgz ../../$tarfile

cd ../../
# cleanup temp directory
rm -rf temp/

exit 0
