module.exports = {
    hooks: {
        readPackage
    }
}

function readPackage (pkg) {
    switch (pkg.name) {
        case 'emotion-theming':
            pkg.dependencies['prop-types'] = '^15.7.2'
            break
        case 'mineral-ui':
            pkg.dependencies['popper.js'] = '^1.15.0'
            break
        case 'create-emotion-styled':
            pkg.dependencies['prop-types'] = '^15.7.2'
            break
        case 'swagger-ui':
            pkg.dependencies['isarray'] = '^2.0.5'
            pkg.dependencies['regenerator-runtime'] = '^0.13.3'
            break

    }
    return pkg
}
