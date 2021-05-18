import React from 'react';
import Container from '@material-ui/core/Container';
import Typography from '@material-ui/core/Typography';
import Box from '@material-ui/core/Box';

export default function App() {
    return (
        <Container maxWidth="sm">
            <Box my={4}>
                <Typography variant="h3" align="center" component="h3" gutterBottom color="primary">
                    Metrics Service
                </Typography>
            </Box>
        </Container>
    );
}
