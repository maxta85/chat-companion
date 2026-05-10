const { getDefaultConfig } = require("@expo/metro-config");
const path = require("path");

// Ensure metro can resolve workspace packages in monorepo setups and
// recognize TS/TSX/CJS extensions used by the project.
const projectRoot = __dirname;
const workspaceRoot = path.resolve(__dirname, "..", "..");

const config = getDefaultConfig(projectRoot);

// Watch the repo root so workspace packages are observed
config.watchFolders = config.watchFolders || [];
if (!config.watchFolders.includes(workspaceRoot)) config.watchFolders.push(workspaceRoot);

// Resolve node_modules from both project and workspace root
config.resolver = config.resolver || {};
config.resolver.nodeModulesPaths = config.resolver.nodeModulesPaths || [];
config.resolver.nodeModulesPaths = Array.from(
  new Set([
    path.join(projectRoot, "node_modules"),
    path.join(workspaceRoot, "node_modules"),
    ...config.resolver.nodeModulesPaths,
  ])
);

// Ensure common extensions are supported
config.resolver.sourceExts = config.resolver.sourceExts || [];
['ts', 'tsx', 'cjs', 'mjs', 'jsx', 'json'].forEach((ext) => {
  if (!config.resolver.sourceExts.includes(ext)) config.resolver.sourceExts.push(ext);
});

module.exports = config;
