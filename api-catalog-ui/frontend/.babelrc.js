import base from '../../.babelrc.json' with { type: 'json' };
export default  {
    ...base,
    env: {
        test: {
            presets: [
                ['@babel/preset-env', { targets: { node: 'current' } }],
                ['@babel/preset-react', { runtime: 'automatic' }],
            ],
            plugins: ['babel-plugin-transform-vite-meta-env'],
        },
    },
};
