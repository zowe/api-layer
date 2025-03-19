import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import PageNotFound from './PageNotFound';
import '@testing-library/jest-dom';
import {BrowserRouter, Route, Routes, useNavigate} from "react-router";

const mockNavigate = jest.fn();
jest.mock('react-router', () => {
    return {
        __esModule: true,
        ...jest.requireActual('react-router'),
        useNavigate: () => mockNavigate,
    };
});

describe('PageNotFound Component', () => {


    it('should render the page not found message and button', () => {

        render(<BrowserRouter>
            <Routes>
                <Route path="*" element= {<PageNotFound />}/>
            </Routes>
        </BrowserRouter>)

        // Check if the text is rendered
        expect(screen.getByText('Page Not Found')).toBeInTheDocument();

        // Check if the button is rendered
        expect(screen.getByTestId('go-home-button')).toBeInTheDocument();
    });

    it('should navigate to /dashboard when the "Go to Dashboard" button is clicked', async () => {

        render(<BrowserRouter>
            <Routes>
                <Route path="*" element= {<PageNotFound />}/>
            </Routes>
        </BrowserRouter>)

        fireEvent.click(screen.getByTestId('go-home-button'));
        await waitFor(() => {
            expect(mockNavigate).toHaveBeenCalled();
            }
        )

    });
});
